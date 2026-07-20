"""
train.py
--------
Trains a LightGBM BINARY classifier using TimeSeriesSplit (walk-forward validation).
Target: UP vs NOT_UP
Temporal order is PRESERVED — no shuffling.

Features (14 total):
    rsi, macd, macd_signal, macd_hist, volatility, zscore, returns_5d,
    atr, bb_pct, vol_roc, vol_ratio, returns_1d, returns_3d, returns_10d

Usage:
    python train.py
    python train.py --input features.csv --folds 5
"""

import argparse
import warnings

import numpy as np
import pandas as pd
from lightgbm import LGBMClassifier
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    roc_auc_score,
)
from sklearn.model_selection import TimeSeriesSplit
from sklearn.preprocessing import LabelEncoder

warnings.filterwarnings("ignore", category=UserWarning)


FEATURE_COLS = [
    # Original 7
    "rsi", "macd", "macd_signal", "macd_hist",
    "volatility", "zscore", "returns_5d",
    # New 7
    "atr", "bb_pct", "vol_roc", "vol_ratio",
    "returns_1d", "returns_3d", "returns_10d",
]
TARGET_COL = "label"

# Reference: previous 3-class walk-forward average
PREV_3CLASS_ACC = 37.41


def parse_args():
    parser = argparse.ArgumentParser(description="Train binary LightGBM with walk-forward CV.")
    parser.add_argument("--input", type=str, default="features.csv", help="Features CSV")
    parser.add_argument("--folds", type=int, default=5,              help="TimeSeriesSplit folds")
    return parser.parse_args()


def print_sep(char="─", width=62):
    print(char * width)


def main():
    args = parse_args()

    # ── Load ─────────────────────────────────────────────────
    print(f"\n📂  Loading: {args.input}")
    df = pd.read_csv(args.input, index_col="Date", parse_dates=True)
    df = df.sort_index()

    # Validate features exist
    missing = [c for c in FEATURE_COLS if c not in df.columns]
    if missing:
        print(f"❌  Missing feature columns: {missing}")
        print(f"   Run build_features.py first to regenerate features.csv")
        return

    print(f"   Total rows  : {len(df)}")
    print(f"   Date range  : {df.index.min().date()} → {df.index.max().date()}")
    print(f"   Features    : {len(FEATURE_COLS)}  →  {FEATURE_COLS}")

    X = df[FEATURE_COLS].values
    y_raw = df[TARGET_COL].values

    le = LabelEncoder()
    y = le.fit_transform(y_raw)   # NOT_UP=0, UP=1
    class_names = list(le.classes_)
    n_classes = len(class_names)

    print(f"   Classes     : {class_names}  (encoded: {list(range(n_classes))})")

    # Baselines
    random_baseline   = 100.0 / n_classes               # 50% for binary
    majority_class    = np.bincount(y).argmax()
    majority_baseline = np.bincount(y).max() / len(y) * 100

    print(f"\n{'═' * 62}")
    print(f"  WALK-FORWARD VALIDATION  ({args.folds} folds, TimeSeriesSplit)")
    print(f"{'═' * 62}")
    print(f"  ⚠  Rows NOT shuffled — temporal order preserved.")
    print(f"  📐  Random baseline    (binary)    = {random_baseline:.1f}%")
    print(f"  📐  Majority baseline  (always '{le.classes_[majority_class]}') = {majority_baseline:.1f}%")
    print(f"  📐  Previous 3-class walk-fwd acc  = {PREV_3CLASS_ACC:.2f}%\n")

    tscv = TimeSeriesSplit(n_splits=args.folds)
    fold_accuracies = []
    fold_aucs       = []

    for fold_idx, (train_idx, test_idx) in enumerate(tscv.split(X), start=1):
        X_train, X_test = X[train_idx], X[test_idx]
        y_train, y_test = y[train_idx], y[test_idx]

        train_start = df.index[train_idx[0]].date()
        train_end   = df.index[train_idx[-1]].date()
        test_start  = df.index[test_idx[0]].date()
        test_end    = df.index[test_idx[-1]].date()

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
        model.fit(
            X_train, y_train,
            eval_set=[(X_test, y_test)],
            callbacks=[],
        )
        y_pred      = model.predict(X_test)
        y_prob      = model.predict_proba(X_test)[:, 1]   # P(UP)

        acc = accuracy_score(y_test, y_pred) * 100
        auc = roc_auc_score(y_test, y_prob) * 100
        fold_accuracies.append(acc)
        fold_aucs.append(auc)

        vs_random   = acc - random_baseline
        vs_majority = acc - majority_baseline

        print_sep()
        print(f"  FOLD {fold_idx}")
        print(f"  Train: {train_start} → {train_end}  ({len(train_idx)} rows)")
        print(f"  Test : {test_start}  → {test_end}   ({len(test_idx)} rows)")
        print(f"  Accuracy : {acc:.2f}%   AUC-ROC : {auc:.2f}%")
        print(f"  vs random baseline (+50%)   : {vs_random:+.2f} pp", end="")
        print("  ✅" if vs_random > 5 else ("  ⚠️" if vs_random > 0 else "  ❌"))
        print(f"  vs majority baseline        : {vs_majority:+.2f} pp")

        print(f"\n  Classification Report:")
        report = classification_report(
            y_test, y_pred, target_names=class_names, digits=3, zero_division=0
        )
        for line in report.splitlines():
            print(f"    {line}")
        print()

    # ── Feature importance (last fold model) ─────────────────
    importances = pd.Series(model.feature_importances_, index=FEATURE_COLS)
    importances = importances.sort_values(ascending=False)

    print_sep("═")
    print("  FEATURE IMPORTANCES  (last fold model)")
    print_sep("═")
    for feat_name, imp in importances.items():
        bar = "█" * int(imp / importances.max() * 30)
        print(f"  {feat_name:<15}  {imp:>6.0f}  {bar}")

    # ── Summary ───────────────────────────────────────────────
    avg_acc = np.mean(fold_accuracies)
    std_acc = np.std(fold_accuracies)
    avg_auc = np.mean(fold_aucs)

    print_sep("═")
    print("  WALK-FORWARD SUMMARY")
    print_sep("═")
    for i, (acc, auc) in enumerate(zip(fold_accuracies, fold_aucs), 1):
        marker = "✅" if acc > random_baseline + 5 else ("⚠️" if acc > random_baseline else "❌")
        print(f"  Fold {i}: Acc={acc:.2f}%  AUC={auc:.2f}%  {marker}")

    print(f"\n  ─── Accuracy ───────────────────────────────────────")
    print(f"  Average walk-fwd accuracy  : {avg_acc:.2f}%  ± {std_acc:.2f}%")
    print(f"  Average AUC-ROC            : {avg_auc:.2f}%")
    print()
    print(f"  ─── Comparison vs all baselines ───────────────────")
    print(f"  Random baseline  (50/50)   : {random_baseline:.1f}%   → lift = {avg_acc - random_baseline:+.2f} pp")
    print(f"  Majority baseline (NOT_UP) : {majority_baseline:.1f}%   → lift = {avg_acc - majority_baseline:+.2f} pp")
    print(f"  Prev 3-class model (37.4%) : {PREV_3CLASS_ACC:.2f}%  → lift = {avg_acc - PREV_3CLASS_ACC:+.2f} pp")
    print()

    # Final verdict
    lift_vs_random = avg_acc - random_baseline
    if avg_auc >= 55 and lift_vs_random >= 5:
        verdict = (
            f"✅  GOOD SIGNAL — {lift_vs_random:.1f} pp above 50% baseline, AUC={avg_auc:.1f}%.\n"
            f"   The 14-feature binary model shows meaningful predictive power.\n"
            f"   Safe to proceed to final_train.py and app.py."
        )
    elif lift_vs_random >= 2:
        verdict = (
            f"⚠️  WEAK-BUT-POSITIVE SIGNAL — {lift_vs_random:.1f} pp above 50% baseline.\n"
            f"   Deployable for experimentation but not production trading."
        )
    else:
        verdict = (
            f"❌  NO MEANINGFUL SIGNAL — Accuracy ({avg_acc:.1f}%) is near 50% baseline.\n"
            f"   Do NOT deploy. Revisit features or label definition."
        )

    print_sep("═")
    print(verdict)
    print_sep("═")


if __name__ == "__main__":
    main()
