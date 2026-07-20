"""
inference_client_example.py
----------------------------
Example Python HTTP client showing how to call the /predict endpoint.
This mirrors what the Android/Kotlin HTTP client will do when integrating
the model into NextPredict.

Prerequisites:
    pip install requests   (already available via pip in the venv)
    uvicorn must be running: uvicorn app:app --port 8000

The script:
  1. Hits /health to confirm the server is up
  2. Sends a LOW-VOLATILITY example row → expects a real signal
  3. Sends a HIGH-VOLATILITY example row → expects NO_RELIABLE_SIGNAL
  Both rows are taken directly from features.csv (real historical data, not invented).

Usage:
    python inference_client_example.py
    python inference_client_example.py --url http://10.0.2.2:8000   # Android emulator
"""

import argparse
import json
import sys

import pandas as pd
import requests

FEATURE_COLS = [
    "rsi", "macd", "macd_signal", "macd_hist",
    "volatility", "zscore", "returns_5d",
    "atr", "bb_pct", "vol_roc", "vol_ratio",
    "returns_1d", "returns_3d", "returns_10d",
]


def parse_args():
    parser = argparse.ArgumentParser(description="Test inference server.")
    parser.add_argument("--url",      type=str, default="http://127.0.0.1:8000")
    parser.add_argument("--features", type=str, default="features.csv")
    return parser.parse_args()


def print_banner(text: str):
    w = 62
    print("\n" + "═" * w)
    print(f"  {text}")
    print("═" * w)


def call_health(base_url: str):
    print_banner("GET /health")
    try:
        r = requests.get(f"{base_url}/health", timeout=5)
        r.raise_for_status()
        data = r.json()
        print(json.dumps(data, indent=2))
        return data.get("status") == "ok"
    except requests.exceptions.ConnectionError:
        print(f"❌  Cannot connect to {base_url}")
        print("   Make sure the server is running:")
        print("   > .\\venv\\Scripts\\uvicorn app:app --port 8000")
        return False


def call_predict(base_url: str, label: str, payload: dict):
    print_banner(f"POST /predict  — {label}")
    print("  Request payload:")
    print(json.dumps(payload, indent=4))
    print()
    try:
        r = requests.post(f"{base_url}/predict", json=payload, timeout=10)
        r.raise_for_status()
        data = r.json()
        print("  Response:")
        print(json.dumps(data, indent=4))

        # Highlight key fields
        sig  = data.get("signal")
        conf = data.get("confidence")
        reg  = data.get("vol_regime")
        vol  = data.get("volatility")
        thr  = data.get("vol_threshold")

        print(f"\n  ── Summary ─────────────────────────────────────────")
        print(f"  signal      : {sig}")
        print(f"  confidence  : {conf}")
        print(f"  vol_regime  : {reg}")
        print(f"  vol={vol:.5f}  threshold={thr:.5f}  "
              f"{'BELOW → SIGNAL' if vol <= thr else 'ABOVE → GATED'}")
        return data
    except requests.exceptions.ConnectionError:
        print(f"❌  Request failed — server not reachable.")
        return None


def pick_example_row(df: pd.DataFrame, high_vol: bool, vol_threshold: float) -> dict:
    """Pick a real row from features.csv above/below the volatility threshold."""
    if high_vol:
        subset = df[df["volatility"] > vol_threshold]
        if subset.empty:
            subset = df.nlargest(5, "volatility")
        row = subset.nlargest(1, "volatility").iloc[0]
        description = f"HIGH-VOL example (volatility={row['volatility']:.5f}, date={subset.nlargest(1,'volatility').index[0].date()})"
    else:
        subset = df[df["volatility"] <= vol_threshold]
        if subset.empty:
            subset = df.nsmallest(5, "volatility")
        # Pick near the middle of low-vol rows for a representative example
        row = subset.iloc[len(subset) // 2]
        description = f"LOW-VOL example (volatility={row['volatility']:.5f}, date={subset.index[len(subset)//2].date()})"

    payload = {col: round(float(row[col]), 6) for col in FEATURE_COLS}
    return payload, description


def main():
    args = parse_args()

    # 1. Health check
    ok = call_health(args.url)
    if not ok:
        sys.exit(1)

    # 2. Load threshold from server health response to pick real examples
    health_data = requests.get(f"{args.url}/health").json()
    vol_threshold = health_data.get("vol_threshold", 0.0145)

    # 3. Load features.csv for real example rows
    try:
        df = pd.read_csv(args.features, index_col="Date", parse_dates=True)
    except FileNotFoundError:
        print(f"⚠️  {args.features} not found — using synthetic example values")
        df = None

    if df is not None:
        # LOW-VOL request
        low_payload, low_desc = pick_example_row(df, high_vol=False, vol_threshold=vol_threshold)
        call_predict(args.url, f"LOW-VOL  → Expect real signal    [{low_desc}]", low_payload)

        # HIGH-VOL request
        high_payload, high_desc = pick_example_row(df, high_vol=True, vol_threshold=vol_threshold)
        call_predict(args.url, f"HIGH-VOL → Expect NO_RELIABLE_SIGNAL  [{high_desc}]", high_payload)
    else:
        # Fallback: synthetic values
        low_payload = {
            "rsi": 54.2, "macd": 8.1, "macd_signal": 6.9, "macd_hist": 1.2,
            "volatility": 0.0120, "zscore": 0.45, "atr": 0.019,
            "bb_pct": 0.62, "returns_5d": 0.012, "returns_1d": 0.003,
            "returns_3d": 0.007, "returns_10d": 0.019,
            "vol_roc": 0.05, "vol_ratio": 0.88,
        }
        high_payload = {**low_payload, "volatility": 0.055}

        call_predict(args.url, "LOW-VOL synthetic  → Expect real signal",      low_payload)
        call_predict(args.url, "HIGH-VOL synthetic → Expect NO_RELIABLE_SIGNAL", high_payload)

    print("\n" + "═" * 62)
    print("  ANDROID INTEGRATION NOTES")
    print("═" * 62)
    print("  Base URL (emulator → host): http://10.0.2.2:8000")
    print("  Base URL (physical device): http://<your-PC-LAN-IP>:8000")
    print()
    print("  Retrofit endpoint:")
    print('    @POST("predict")')
    print("    suspend fun predict(@Body req: PredictRequest): PredictResponse")
    print()
    print("  PredictResponse fields to consume in Kotlin:")
    print("    signal       : String   // 'UP', 'NOT_UP', 'NO_RELIABLE_SIGNAL'")
    print("    confidence   : Double?  // null when gated")
    print("    vol_regime   : String   // 'low_medium' or 'high'")
    print("    note         : String   // disclaimer text to show in UI")
    print("    timestamp    : String   // ISO-8601 UTC")
    print("═" * 62)


if __name__ == "__main__":
    main()
