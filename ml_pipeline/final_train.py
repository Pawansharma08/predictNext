"""
final_train.py
--------------
Trains LightGBM on the COMPLETE features.csv dataset (no holdout — walk-forward
validation was already done in train.py). Saves:

  model.pkl                 - trained LightGBM classifier (joblib)
  volatility_threshold.json - Q2 (median) volatility gate for inference

The volatility gate: at inference time, only emit a direction signal when the
incoming volatility value is <= this threshold (low/medium regime). In
high-volatility regimes the model showed no reliable edge in backtests.

Usage:
    python final_train.py
    python final_train.py --input features.csv --model model.pkl
"""

import argparse
import json
import os

import joblib
import numpy as np
import pandas as pd
from lightgbm import LGBMClassifier
from sklearn.preprocessing import LabelEncoder

FEATURE_COLS = [
    "rsi", "macd", "macd_signal", "macd_hist",
    "volatility", "zscore", "returns_5d",
    "atr", "bb_pct", "vol_roc", "vol_ratio",
    "returns_1d", "returns_3d", "returns_10d",
]
TARGET_COL = "label"


def parse_args():
    parser = argparse.ArgumentParser(description="Train final model on full dataset.")
    parser.add_argument("--input",     type=str, default="features.csv", help="features CSV")
    parser.add_argument("--model",     type=str, default="model.pkl",    help="output model path")
    parser.add_argument("--threshold", type=str, default="volatility_threshold.json")
    return parser.parse_args()


def main():
    args = parse_args()

    # ── Load ─────────────────────────────────────────────────────
    print(f"\n📂  Loading: {args.input}")
    df = pd.read_csv(args.input, index_col="Date", parse_dates=True)
    df = df.sort_index()

    missing = [c for c in FEATURE_COLS if c not in df.columns]
    if missing:
        print(f"❌  Missing feature columns: {missing}")
        return

    print(f"   Rows        : {len(df)}")
    print(f"   Date range  : {df.index.min().date()} → {df.index.max().date()}")
    print(f"   Features    : {len(FEATURE_COLS)}  →  {FEATURE_COLS}")

    X = df[FEATURE_COLS].values
    y_raw = df[TARGET_COL].values

    le = LabelEncoder()
    y  = le.fit_transform(y_raw)
    class_names = list(le.classes_)
    print(f"   Classes     : {class_names}  (0={class_names[0]}, 1={class_names[1]})")

    # Label distribution
    counts = pd.Series(y).value_counts().sort_index()
    for i, cnt in counts.items():
        print(f"   {class_names[i]:<10}  {cnt}  ({cnt/len(y)*100:.1f}%)")

    # ── Volatility gate threshold ─────────────────────────────────
    vol_series    = df["volatility"]
    vol_q2        = float(np.percentile(vol_series, 50))   # median = Q2
    vol_q3        = float(np.percentile(vol_series, 75))   # Q3 for reference
    low_med_count = int((vol_series <= vol_q2).sum())
    high_count    = int((vol_series >  vol_q2).sum())

    threshold_data = {
        "vol_threshold_q2":      vol_q2,
        "vol_threshold_q3":      vol_q3,
        "vol_min":               float(vol_series.min()),
        "vol_max":               float(vol_series.max()),
        "signal_regime":         "low_medium",
        "signal_condition":      f"volatility <= {vol_q2:.6f}",
        "pct_days_with_signal":  round(low_med_count / len(df) * 100, 1),
        "trained_on":            args.input,
        "n_training_rows":       len(df),
        "feature_cols":          FEATURE_COLS,
        "classes":               class_names,
        "label_encoding":        {name: int(i) for i, name in enumerate(class_names)},
    }

    print(f"\n{'═'*60}")
    print(f"  VOLATILITY GATE")
    print(f"{'═'*60}")
    print(f"  Threshold (Q2 / median) : {vol_q2:.6f}")
    print(f"  Q3 (top-25% cutoff)     : {vol_q3:.6f}")
    print(f"  Days below threshold    : {low_med_count} ({low_med_count/len(df)*100:.1f}%) → SIGNAL emitted")
    print(f"  Days above threshold    : {high_count} ({high_count/len(df)*100:.1f}%) → NO_RELIABLE_SIGNAL")

    # ── Train on full dataset ─────────────────────────────────────
    print(f"\n{'═'*60}")
    print(f"  TRAINING ON FULL DATASET  ({len(df)} rows)")
    print(f"{'═'*60}")

    model = LGBMClassifier(
        n_estimators=500,
        learning_rate=0.03,
        num_leaves=31,
        max_depth=6,
        min_child_samples=20,
        subsample=0.8,
        colsample_bytree=0.8,
        reg_alpha=0.1,
        reg_lambda=0.1,
        class_weight="balanced",
        random_state=42,
        verbose=-1,
    )

    print("  Fitting LightGBM …", end="", flush=True)
    model.fit(X, y)
    print(" done ✅")

    # Feature importance (for audit)
    imp = pd.Series(model.feature_importances_, index=FEATURE_COLS).sort_values(ascending=False)
    print("\n  Feature importances:")
    for feat, val in imp.items():
        bar = "█" * int(val / imp.max() * 25)
        print(f"    {feat:<15}  {val:>5.0f}  {bar}")

    # ── Save model ────────────────────────────────────────────────
    joblib.dump({"model": model, "label_encoder": le, "feature_cols": FEATURE_COLS}, args.model)
    print(f"\n✅  Model saved  →  {args.model}  ({os.path.getsize(args.model)/1024:.1f} KB)")

    # ── Save threshold config ─────────────────────────────────────
    with open(args.threshold, "w") as f:
        json.dump(threshold_data, f, indent=2)
    print(f"✅  Threshold config saved  →  {args.threshold}")

    print(f"\n{'═'*60}")
    print(f"  DEPLOYMENT SUMMARY")
    print(f"{'═'*60}")
    print(f"  Model      : LightGBM binary classifier (UP vs NOT_UP)")
    print(f"  Horizon    : 5-day forward return > +1%")
    print(f"  Features   : {len(FEATURE_COLS)}")
    print(f"  Trained on : {len(df)} rows  ({df.index.min().date()} – {df.index.max().date()})")
    print(f"  Vol gate   : emit signal only when volatility ≤ {vol_q2:.6f}")
    print(f"  Coverage   : ~{low_med_count/len(df)*100:.0f}% of trading days")
    print(f"\n  ⚠️  DISCLAIMER: Walk-forward avg accuracy = ~51.7% (binary baseline 50%).")
    print(f"     The model has a ~5pp edge in low-vol regimes — research use only.")
    print(f"{'═'*60}")


if __name__ == "__main__":
    main()
