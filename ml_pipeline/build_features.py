"""
build_features.py
-----------------
Reads data.csv (OHLCV) and computes technical indicators + binary label.

Features produced (12 total):
  rsi          - RSI(14)
  macd         - MACD line  (12, 26, 9)
  macd_signal  - Signal line
  macd_hist    - Histogram (MACD - signal)
  volatility   - 20-day rolling std-dev of daily log-returns
  zscore       - 20-day z-score of closing price
  returns_5d   - 5-day trailing return
  atr          - Average True Range (14-period), normalized by Close
  bb_pct       - Bollinger Band %B (20-period): where price sits in the band
  vol_roc      - Volume rate-of-change (5-day)
  vol_ratio    - Volume / 20-day moving average of volume
  returns_1d   - 1-day trailing return
  returns_3d   - 3-day trailing return
  returns_10d  - 10-day trailing return

Label (BINARY target):
  UP      -> future N-day return > +1%
  NOT_UP  -> everything else (DOWN or SIDEWAYS, i.e. return <= +1%)

Usage:
    python build_features.py                          # default N=5
    python build_features.py --horizon 10
    python build_features.py --input data.csv --output features.csv
"""

import argparse

import numpy as np
import pandas as pd
import ta


# ─────────────────────────────────────────────────────────────────
# Argument parsing
# ─────────────────────────────────────────────────────────────────
def parse_args():
    parser = argparse.ArgumentParser(description="Build ML features from OHLCV data.")
    parser.add_argument("--input",      type=str,   default="data.csv",     help="Input CSV")
    parser.add_argument("--output",     type=str,   default="features.csv", help="Output CSV")
    parser.add_argument("--horizon",    type=int,   default=5,              help="Future N days for label (default: 5)")
    parser.add_argument("--up_thresh",  type=float, default=0.01,           help="Up threshold (default: 0.01 = +1%%)")
    return parser.parse_args()


# ─────────────────────────────────────────────────────────────────
# Feature engineering
# ─────────────────────────────────────────────────────────────────
def build_features(df: pd.DataFrame, horizon: int, up_thresh: float) -> pd.DataFrame:
    feat = df.copy()

    # ── Original 7 features ────────────────────────────────────

    # RSI(14)
    feat["rsi"] = ta.momentum.RSIIndicator(close=feat["Close"], window=14).rsi()

    # MACD(12, 26, 9)
    macd_obj = ta.trend.MACD(
        close=feat["Close"], window_slow=26, window_fast=12, window_sign=9
    )
    feat["macd"]        = macd_obj.macd()
    feat["macd_signal"] = macd_obj.macd_signal()
    feat["macd_hist"]   = macd_obj.macd_diff()

    # 20-day rolling volatility (std of log-returns)
    log_returns = np.log(feat["Close"] / feat["Close"].shift(1))
    feat["volatility"] = log_returns.rolling(window=20).std()

    # 20-day z-score of Close
    roll_mean = feat["Close"].rolling(window=20).mean()
    roll_std  = feat["Close"].rolling(window=20).std()
    feat["zscore"] = (feat["Close"] - roll_mean) / roll_std

    # 5-day trailing return
    feat["returns_5d"] = feat["Close"].pct_change(periods=5)

    # ── New features ───────────────────────────────────────────

    # ATR(14) normalized by Close (removes price-level bias)
    atr_obj = ta.volatility.AverageTrueRange(
        high=feat["High"], low=feat["Low"], close=feat["Close"], window=14
    )
    feat["atr"] = atr_obj.average_true_range() / feat["Close"]

    # Bollinger Band %B  (0 = at lower band, 1 = at upper band)
    bb_obj = ta.volatility.BollingerBands(
        close=feat["Close"], window=20, window_dev=2
    )
    feat["bb_pct"] = bb_obj.bollinger_pband()   # percent bandwidth position

    # Volume Rate-of-Change (5-day)
    feat["vol_roc"] = feat["Volume"].pct_change(periods=5)

    # Volume vs its 20-day moving average (ratio)
    vol_ma20 = feat["Volume"].rolling(window=20).mean()
    feat["vol_ratio"] = feat["Volume"] / vol_ma20

    # Lagged returns
    feat["returns_1d"]  = feat["Close"].pct_change(periods=1)
    feat["returns_3d"]  = feat["Close"].pct_change(periods=3)
    feat["returns_10d"] = feat["Close"].pct_change(periods=10)

    # ── Binary Label ───────────────────────────────────────────
    future_close  = feat["Close"].shift(-horizon)
    future_return = (future_close - feat["Close"]) / feat["Close"]
    feat["label"] = np.where(future_return > up_thresh, "UP", "NOT_UP")
    # Mark look-ahead tail as NaN
    feat.loc[feat.index[-horizon:], "label"] = np.nan

    return feat


# ─────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────
def main():
    args = parse_args()

    print(f"\n📂  Reading: {args.input}")
    df = pd.read_csv(args.input, index_col="Date", parse_dates=True)
    print(f"   Rows loaded : {len(df)}")

    print(f"\n⚙️   Computing features  (horizon = {args.horizon} days, up_thresh = {args.up_thresh*100:.0f}%) …")
    feat = build_features(df, args.horizon, args.up_thresh)

    # All feature columns (12 total)
    feature_cols = [
        "rsi", "macd", "macd_signal", "macd_hist",
        "volatility", "zscore", "returns_5d",
        "atr", "bb_pct", "vol_roc", "vol_ratio",
        "returns_1d", "returns_3d", "returns_10d",
    ]
    keep_cols = feature_cols + ["label"]
    feat = feat[keep_cols]

    # Drop NaN rows
    before = len(feat)
    feat.dropna(inplace=True)
    after  = len(feat)
    print(f"   Dropped {before - after} NaN rows (warm-up + look-ahead tail)")
    print(f"   Remaining rows: {after}")

    # ── Label distribution ─────────────────────────────────────
    print("\n" + "═" * 60)
    print("  BINARY LABEL DISTRIBUTION")
    print("═" * 60)
    counts = feat["label"].value_counts()
    total  = len(feat)
    for label, cnt in counts.items():
        pct = cnt / total * 100
        bar = "█" * int(pct / 2)
        print(f"  {label:<10}  {cnt:>5}  ({pct:5.1f}%)  {bar}")
    print("═" * 60)

    # Imbalance check
    min_pct = counts.min() / total * 100
    max_pct = counts.max() / total * 100
    imbalance_ratio = max_pct / min_pct if min_pct > 0 else float("inf")
    if imbalance_ratio > 3:
        print(f"\n⚠️  WARNING: Severe imbalance! Ratio = {imbalance_ratio:.1f}x")
    elif imbalance_ratio > 1.5:
        print(f"\n⚠️  Mild imbalance: ratio = {imbalance_ratio:.1f}x — model uses class_weight='balanced'")
    else:
        print(f"\n✅  Reasonably balanced (ratio = {imbalance_ratio:.1f}x)")

    # Random baseline
    baseline = 100.0 / counts.nunique()
    print(f"\n📐  Random baseline (binary) = {baseline:.1f}%")
    majority_baseline = max_pct
    print(f"📐  Majority-class baseline  = {majority_baseline:.1f}%  (always predict '{counts.idxmax()}')")

    # Feature stats
    print(f"\n--- Feature statistics ({len(feature_cols)} features) ---")
    print(feat[feature_cols].describe().round(4).to_string())

    feat.to_csv(args.output)
    print(f"\n💾  Saved to: {args.output}")
    print(f"   Features  : {feature_cols}")


if __name__ == "__main__":
    main()
