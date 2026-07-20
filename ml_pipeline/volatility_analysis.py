"""
volatility_analysis.py
----------------------
Tests whether the model's predictive edge is concentrated in high-volatility
periods by evaluating the 5-day model separately on different volatility regimes.

Methodology:
  - Uses the same TimeSeriesSplit (5 folds) as train.py
  - Volatility quartile thresholds are computed PER FOLD on TRAINING data only
    (no look-ahead bias — we don't peek at test-set volatility distribution)
  - For each fold, the model is trained on ALL training rows
  - Predictions are then evaluated on 3 subsets of the TEST rows:
      ALL     : every test row (full baseline)
      Q4      : test rows where volatility >= Q4 threshold (top 25% vol)
      Q1+Q2   : test rows where volatility <= Q2 threshold (bottom 50% vol)

Usage:
    python volatility_analysis.py
    python volatility_analysis.py --input features.csv --folds 5
"""

import argparse
import warnings

import numpy as np
import pandas as pd
from lightgbm import LGBMClassifier
from sklearn.metrics import accuracy_score, roc_auc_score
from sklearn.model_selection import TimeSeriesSplit
from sklearn.preprocessing import LabelEncoder

warnings.filterwarnings("ignore", category=UserWarning)

FEATURE_COLS = [
    "rsi", "macd", "macd_signal", "macd_hist",
    "volatility", "zscore", "returns_5d",
    "atr", "bb_pct", "vol_roc", "vol_ratio",
    "returns_1d", "returns_3d", "returns_10d",
]
TARGET_COL = "label"
VOL_COL    = "volatility"


def parse_args():
    parser = argparse.ArgumentParser(description="Volatility-regime walk-forward analysis.")
    parser.add_argument("--input", type=str, default="features.csv")
    parser.add_argument("--folds", type=int, default=5)
    return parser.parse_args()


def safe_auc(y_true, y_prob):
    """AUC requires both classes present in y_true."""
    if len(np.unique(y_true)) < 2:
        return float("nan")
    return roc_auc_score(y_true, y_prob) * 100


def evaluate_subset(y_true, y_pred, y_prob, label):
    """Return (n, acc, auc) for a subset; returns NaN if too few samples."""
    n = len(y_true)
    if n < 10:
        return n, float("nan"), float("nan")
    acc = accuracy_score(y_true, y_pred) * 100
    auc = safe_auc(y_true, y_prob)
    return n, acc, auc


def sep(char="─", w=68):
    print(char * w)


def main():
    args = parse_args()

    # ── Load ─────────────────────────────────────────────────────────
    print(f"\n📂  Loading: {args.input}")
    df = pd.read_csv(args.input, index_col="Date", parse_dates=True)
    df = df.sort_index()

    missing = [c for c in FEATURE_COLS if c not in df.columns]
    if missing:
        print(f"❌  Missing columns: {missing}")
        return

    print(f"   Rows: {len(df)}  |  {df.index.min().date()} → {df.index.max().date()}")

    X       = df[FEATURE_COLS].values
    vol_all = df[VOL_COL].values
    y_raw   = df[TARGET_COL].values

    le = LabelEncoder()
    y  = le.fit_transform(y_raw)
    class_names  = list(le.classes_)
    random_base  = 50.0

    # ── Full-dataset volatility distribution (for reporting only) ────
    q1_full = np.percentile(vol_all, 25)
    q2_full = np.percentile(vol_all, 50)
    q3_full = np.percentile(vol_all, 75)

    n_q4_all = np.sum(vol_all >= q3_full)
    n_q12_all = np.sum(vol_all <= q2_full)
    pct_q4   = n_q4_all / len(vol_all) * 100

    print(f"\n{'═'*68}")
    print("  VOLATILITY DISTRIBUTION  (full dataset)")
    print(f"{'═'*68}")
    print(f"  Volatility = 20-day rolling std-dev of log-returns")
    print(f"  Min  : {vol_all.min():.5f}    Max  : {vol_all.max():.5f}")
    print(f"  Q1   : {q1_full:.5f}    Q2   : {q2_full:.5f}    Q3   : {q3_full:.5f}")
    print()
    print(f"  Q4 (top 25% vol, >= {q3_full:.5f})")
    print(f"    → {n_q4_all} rows  ({pct_q4:.1f}% of all trading days)")
    print(f"  Q1+Q2 (bottom 50% vol, <= {q2_full:.5f})")
    print(f"    → {n_q12_all} rows  ({n_q12_all/len(vol_all)*100:.1f}% of all trading days)")
    print()
    print(f"  ⚠️  PRACTICAL LIMIT: A strategy only usable on Q4 days is active")
    print(f"     ~{pct_q4:.0f}% of the time — roughly {int(252*pct_q4/100)} trading days per year.")
    print(f"     Additionally, Q4 is identified using PAST 20-day vol, so it IS")
    print(f"     available in real-time (no future look-ahead). However, it")
    print(f"     clusters around crash periods (COVID 2020, etc.) which are")
    print(f"     rare, non-recurring, and hard to trade in practice.")
    print(f"{'═'*68}")

    # ── Walk-forward with per-fold quartile thresholds ───────────────
    tscv = TimeSeriesSplit(n_splits=args.folds)

    # Accumulators
    results = {
        "ALL":   {"acc": [], "auc": [], "n": []},
        "Q4":    {"acc": [], "auc": [], "n": []},
        "Q1+Q2": {"acc": [], "auc": [], "n": []},
    }

    print(f"\n  WALK-FORWARD  ({args.folds} folds, TimeSeriesSplit)")
    print(f"  Vol thresholds computed PER FOLD on training data only (no leakage)\n")

    for fold_idx, (train_idx, test_idx) in enumerate(tscv.split(X), start=1):
        X_train, X_test = X[train_idx], X[test_idx]
        y_train, y_test = y[train_idx], y[test_idx]
        vol_test        = vol_all[test_idx]

        # ── Thresholds from TRAINING data only ──────────────────────
        vol_train = vol_all[train_idx]
        q2_thresh = np.percentile(vol_train, 50)   # bottom 50%
        q3_thresh = np.percentile(vol_train, 75)   # top 25% cutoff

        # Masks for test rows
        mask_q4   = vol_test >= q3_thresh
        mask_q12  = vol_test <= q2_thresh

        # ── Train model on all training rows ────────────────────────
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
        model.fit(X_train, y_train)

        y_pred_all = model.predict(X_test)
        y_prob_all = model.predict_proba(X_test)[:, 1]

        train_start = df.index[train_idx[0]].date()
        train_end   = df.index[train_idx[-1]].date()
        test_start  = df.index[test_idx[0]].date()
        test_end    = df.index[test_idx[-1]].date()

        sep()
        print(f"  FOLD {fold_idx}  |  Train: {train_start}→{train_end}  "
              f"Test: {test_start}→{test_end}")
        print(f"  Vol thresholds (from train): Q2={q2_thresh:.5f}  Q3={q3_thresh:.5f}")
        sep("·")

        for label, mask in [("ALL", np.ones(len(y_test), dtype=bool)),
                             ("Q4",  mask_q4),
                             ("Q1+Q2", mask_q12)]:
            if mask.sum() == 0:
                print(f"  {label:<6}  → 0 rows in this fold — skipped")
                results[label]["acc"].append(float("nan"))
                results[label]["auc"].append(float("nan"))
                results[label]["n"].append(0)
                continue

            yt = y_test[mask]
            yp = y_pred_all[mask]
            ypr = y_prob_all[mask]
            n, acc, auc = evaluate_subset(yt, yp, ypr, label)

            up_pct = yt.mean() * 100  # % of UP labels in this subset/fold

            acc_str = f"{acc:.2f}%" if not np.isnan(acc) else "N/A"
            auc_str = f"{auc:.2f}%" if not np.isnan(auc) else "N/A"
            lift    = (acc - random_base) if not np.isnan(acc) else float("nan")
            lift_str = f"{lift:+.2f} pp" if not np.isnan(lift) else "N/A"
            flag    = ("✅" if lift > 5 else ("⚠️" if lift > 0 else "❌")) if not np.isnan(lift) else "—"

            print(f"  {label:<6}  n={n:>4}  UP={up_pct:4.1f}%  "
                  f"Acc={acc_str}  AUC={auc_str}  "
                  f"lift={lift_str}  {flag}")

            results[label]["acc"].append(acc)
            results[label]["auc"].append(auc)
            results[label]["n"].append(n)

        print()

    # ── Summary ──────────────────────────────────────────────────────
    sep("═")
    print("  SUMMARY TABLE")
    sep("═")
    header = f"  {'Subset':<10} {'Avg N/fold':>10} {'Avg Acc':>10} {'Avg AUC':>10} {'Lift vs 50%':>12}"
    print(header)
    sep("·")

    for label in ["ALL", "Q4", "Q1+Q2"]:
        acc_vals = [v for v in results[label]["acc"] if not np.isnan(v)]
        auc_vals = [v for v in results[label]["auc"] if not np.isnan(v)]
        n_vals   = [v for v in results[label]["n"]   if v > 0]

        avg_acc = np.mean(acc_vals) if acc_vals else float("nan")
        avg_auc = np.mean(auc_vals) if auc_vals else float("nan")
        avg_n   = np.mean(n_vals)   if n_vals   else 0
        lift    = avg_acc - random_base if not np.isnan(avg_acc) else float("nan")

        acc_str  = f"{avg_acc:.2f}%" if not np.isnan(avg_acc) else "N/A"
        auc_str  = f"{avg_auc:.2f}%" if not np.isnan(avg_auc) else "N/A"
        lift_str = f"{lift:+.2f} pp" if not np.isnan(lift)    else "N/A"
        flag     = ("✅" if lift > 5 else ("⚠️" if lift > 0 else "❌")) if not np.isnan(lift) else "—"

        print(f"  {label:<10} {avg_n:>10.0f} {acc_str:>10} {auc_str:>10} {lift_str:>12}  {flag}")

    sep("═")
    print()
    print("  INTERPRETATION")
    sep("·")

    # Auto-interpret
    q4_acc_vals = [v for v in results["Q4"]["acc"] if not np.isnan(v)]
    q12_acc_vals = [v for v in results["Q1+Q2"]["acc"] if not np.isnan(v)]
    all_acc_vals = [v for v in results["ALL"]["acc"] if not np.isnan(v)]

    q4_avg   = np.mean(q4_acc_vals)   if q4_acc_vals   else float("nan")
    q12_avg  = np.mean(q12_acc_vals)  if q12_acc_vals  else float("nan")
    all_avg  = np.mean(all_acc_vals)  if all_acc_vals  else float("nan")

    q4_lift  = q4_avg  - random_base if not np.isnan(q4_avg)  else float("nan")
    q12_lift = q12_avg - random_base if not np.isnan(q12_avg) else float("nan")

    print(f"  Q4  (high-vol) lift vs 50%:    {q4_lift:+.2f} pp")
    print(f"  Q1+Q2 (low-vol) lift vs 50%:  {q12_lift:+.2f} pp")
    print(f"  ALL data lift vs 50%:          {all_avg - random_base:+.2f} pp")
    print()

    if not np.isnan(q4_lift) and not np.isnan(q12_lift):
        edge_concentrated = q4_lift > (all_avg - random_base) * 1.5

        if edge_concentrated and q4_lift > 3:
            print(f"  ✅  CONFIRMED: Model's edge IS concentrated in high-volatility")
            print(f"     regimes (Q4 lift = {q4_lift:+.1f} pp vs ALL lift = "
                  f"{all_avg-random_base:+.1f} pp).")
        elif q4_lift <= 1 and q12_lift <= 1:
            print(f"  ❌  NO REGIME CONCENTRATION: Edge is uniformly weak across")
            print(f"     both high-vol and low-vol periods. The model lacks a")
            print(f"     reliable volatility-based filter.")
        else:
            print(f"  ⚠️  MIXED: Slight edge concentration in Q4 but not strong enough")
            print(f"     to build a reliable volatility-filtered strategy.")

        print()
        print(f"  PRACTICAL LIMITS OF A Q4-ONLY STRATEGY:")
        print(f"  • Active only {pct_q4:.0f}% of trading days "
              f"(~{int(252*pct_q4/100)} days/year)")
        print(f"  • Clusters around rare crash events (COVID 2020, etc.)")
        print(f"  • Volatility IS observable in real-time (no look-ahead)")
        print(f"  • But high-vol periods are precisely when slippage &")
        print(f"    execution risk are highest — model accuracy alone is not")
        print(f"    sufficient to determine tradability.")

    sep("═")


if __name__ == "__main__":
    main()
