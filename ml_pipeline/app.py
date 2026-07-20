"""
app.py
------
FastAPI inference server for the NSE stock direction prediction model.

Endpoints:
  GET  /health                → uptime / model status check
  POST /predict               → direction signal (pre-computed 14 features)
  POST /compute-and-predict   → Option B: send raw OHLCV candles, server computes
                                features internally and returns ML signal.
                                This is what the Android app calls — it forwards
                                the Yahoo Finance OHLCV data it already fetches.

Volatility gate:
  - If incoming `volatility` <= threshold (Q2 from training data):
    → Low/medium regime → run model → return UP or NOT_UP + confidence
  - If incoming `volatility` > threshold:
    → High-volatility regime → return NO_RELIABLE_SIGNAL (backtests show no edge here)

Start:
    uvicorn app:app --host 0.0.0.0 --port 8000 --reload
"""

import json
import os
from datetime import datetime, timezone
from typing import List, Optional

import joblib
import numpy as np
import pandas as pd
import ta
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# ── Paths ────────────────────────────────────────────────────────
MODEL_PATH     = "model.pkl"
THRESHOLD_PATH = "volatility_threshold.json"

# ── Load model & config at startup ───────────────────────────────
_bundle    = None
_threshold = None
_feature_cols = None


def load_artifacts():
    global _bundle, _threshold, _feature_cols
    if not os.path.exists(MODEL_PATH):
        raise RuntimeError(f"Model not found: {MODEL_PATH}. Run final_train.py first.")
    if not os.path.exists(THRESHOLD_PATH):
        raise RuntimeError(f"Threshold config not found: {THRESHOLD_PATH}. Run final_train.py first.")

    _bundle       = joblib.load(MODEL_PATH)
    _feature_cols = _bundle["feature_cols"]

    with open(THRESHOLD_PATH) as f:
        _threshold = json.load(f)

    print(f"✅  Model loaded from {MODEL_PATH}")
    print(f"✅  Volatility gate: <= {_threshold['vol_threshold_q2']:.6f}  "
          f"(covers ~{_threshold['pct_days_with_signal']}% of trading days)")


# ── App ───────────────────────────────────────────────────────────
app = FastAPI(
    title="NSE Stock Direction Predictor",
    description=(
        "Binary UP/NOT_UP prediction for NSE-listed stocks using LightGBM + "
        "14 technical features. Signal only emitted in low/medium volatility "
        "regimes where backtests showed ~5pp edge. Not investment advice."
    ),
    version="1.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # tighten in production
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup_event():
    load_artifacts()


# ── /predict request/response schemas ────────────────────────────
class PredictRequest(BaseModel):
    rsi:         float = Field(..., description="RSI(14)", ge=0, le=100)
    macd:        float = Field(..., description="MACD line (12,26,9)")
    macd_signal: float = Field(..., description="MACD signal line")
    macd_hist:   float = Field(..., description="MACD histogram")
    volatility:  float = Field(..., description="20-day rolling std-dev of log-returns", ge=0)
    zscore:      float = Field(..., description="20-day z-score of Close")
    atr:         float = Field(..., description="ATR(14) normalised by Close", ge=0)
    bb_pct:      float = Field(..., description="Bollinger Band %B")
    returns_5d:  float = Field(..., description="5-day trailing return")
    returns_1d:  float = Field(..., description="1-day trailing return")
    returns_3d:  float = Field(..., description="3-day trailing return")
    returns_10d: float = Field(..., description="10-day trailing return")
    vol_roc:     float = Field(..., description="Volume rate-of-change 5d")
    vol_ratio:   float = Field(..., description="Volume / 20-day MA volume", ge=0)

    class Config:
        json_schema_extra = {
            "example": {
                "rsi": 58.3, "macd": 12.5, "macd_signal": 10.1, "macd_hist": 2.4,
                "volatility": 0.0132, "zscore": 0.85, "atr": 0.021, "bb_pct": 0.68,
                "returns_5d": 0.018, "returns_1d": 0.004, "returns_3d": 0.009,
                "returns_10d": 0.025, "vol_roc": 0.12, "vol_ratio": 0.95,
            }
        }


class PredictResponse(BaseModel):
    signal:       str
    confidence:   Optional[float]
    vol_regime:   str
    volatility:   float
    vol_threshold: float
    note:         str
    timestamp:    str


# ── /compute-and-predict schemas ─────────────────────────────────
class OhlcvCandle(BaseModel):
    """Single daily OHLCV candle — mirrors HistoricalDataPoint in Kotlin."""
    date:   str
    open:   float
    high:   float
    low:    float
    close:  float
    volume: int


class ComputeAndPredictRequest(BaseModel):
    symbol:  str = Field(..., description="NSE ticker e.g. RELIANCE.NS")
    candles: List[OhlcvCandle] = Field(
        ...,
        description="Daily OHLCV candles, oldest first. Minimum 30 required.",
    )

    class Config:
        json_schema_extra = {
            "example": {
                "symbol": "RELIANCE.NS",
                "candles": [
                    {"date": "2024-06-01", "open": 1280.0, "high": 1295.0,
                     "low": 1275.0, "close": 1290.0, "volume": 12000000},
                ]
            }
        }


class MlSignalResponse(BaseModel):
    symbol:        str
    signal:        str            # "UP" | "NOT_UP" | "NO_RELIABLE_SIGNAL" | "INSUFFICIENT_DATA"
    confidence:    Optional[float]
    vol_regime:    str            # "low_medium" | "high" | "unknown"
    volatility:    Optional[float]
    vol_threshold: float
    candles_used:  int
    note:          str
    timestamp:     str


# ── Feature computation (shared logic with build_features.py) ─────
def _compute_features_from_candles(candles: List[OhlcvCandle]) -> Optional[dict]:
    """
    Compute all 14 TA features from raw OHLCV candles.
    Returns None if data is insufficient for indicator warm-up.
    Requires at least 27 candles (MACD slow=26); 50+ recommended.
    """
    df = pd.DataFrame([{
        "Open": c.open, "High": c.high, "Low": c.low,
        "Close": c.close, "Volume": c.volume,
    } for c in candles])

    if len(df) < 27:
        return None

    # RSI(14)
    df["rsi"] = ta.momentum.RSIIndicator(close=df["Close"], window=14).rsi()

    # MACD(12,26,9)
    macd_obj = ta.trend.MACD(close=df["Close"], window_slow=26, window_fast=12, window_sign=9)
    df["macd"]        = macd_obj.macd()
    df["macd_signal"] = macd_obj.macd_signal()
    df["macd_hist"]   = macd_obj.macd_diff()

    # 20-day rolling volatility (std of log-returns)
    log_ret = np.log(df["Close"] / df["Close"].shift(1))
    df["volatility"] = log_ret.rolling(20).std()

    # 20-day z-score of Close
    roll_mean = df["Close"].rolling(20).mean()
    roll_std  = df["Close"].rolling(20).std()
    df["zscore"] = (df["Close"] - roll_mean) / roll_std

    # Trailing returns
    df["returns_5d"]  = df["Close"].pct_change(5)
    df["returns_1d"]  = df["Close"].pct_change(1)
    df["returns_3d"]  = df["Close"].pct_change(3)
    df["returns_10d"] = df["Close"].pct_change(10)

    # ATR(14) normalised by Close (removes price-scale bias)
    atr_obj   = ta.volatility.AverageTrueRange(
        high=df["High"], low=df["Low"], close=df["Close"], window=14)
    df["atr"] = atr_obj.average_true_range() / df["Close"]

    # Bollinger Band %B
    bb_obj      = ta.volatility.BollingerBands(close=df["Close"], window=20, window_dev=2)
    df["bb_pct"] = bb_obj.bollinger_pband()

    # Volume features
    df["vol_roc"]   = df["Volume"].pct_change(5)
    vol_ma20        = df["Volume"].rolling(20).mean()
    df["vol_ratio"] = df["Volume"] / vol_ma20

    feature_cols = [
        "rsi", "macd", "macd_signal", "macd_hist",
        "volatility", "zscore", "returns_5d",
        "atr", "bb_pct", "vol_roc", "vol_ratio",
        "returns_1d", "returns_3d", "returns_10d",
    ]
    clean = df[feature_cols].dropna()
    if clean.empty:
        return None
    return clean.iloc[-1].to_dict()    # most recent complete row


# ── Endpoints ─────────────────────────────────────────────────────
@app.get("/health")
async def health():
    model_ok = _bundle is not None and _threshold is not None
    return {
        "status":                "ok" if model_ok else "degraded",
        "model_loaded":          model_ok,
        "vol_threshold":         _threshold["vol_threshold_q2"] if _threshold else None,
        "pct_days_with_signal":  _threshold["pct_days_with_signal"] if _threshold else None,
        "features":              _feature_cols if _feature_cols else None,
        "timestamp":             datetime.now(timezone.utc).isoformat(),
    }


@app.post("/predict", response_model=PredictResponse)
async def predict(req: PredictRequest):
    """Predict from pre-computed feature values (for testing / direct integration)."""
    if _bundle is None or _threshold is None:
        raise HTTPException(status_code=503, detail="Model not loaded.")

    vol_threshold = _threshold["vol_threshold_q2"]
    ts = datetime.now(timezone.utc).isoformat()

    if req.volatility > vol_threshold:
        return PredictResponse(
            signal        = "NO_RELIABLE_SIGNAL",
            confidence    = None,
            vol_regime    = "high",
            volatility    = req.volatility,
            vol_threshold = vol_threshold,
            note          = (
                f"High-volatility regime (vol={req.volatility:.5f} > threshold={vol_threshold:.5f}). "
                "Technical indicators become unreliable during sharp moves."
            ),
            timestamp     = ts,
        )

    model = _bundle["model"]
    le    = _bundle["label_encoder"]
    feature_values = [
        req.rsi, req.macd, req.macd_signal, req.macd_hist,
        req.volatility, req.zscore, req.returns_5d,
        req.atr, req.bb_pct, req.vol_roc, req.vol_ratio,
        req.returns_1d, req.returns_3d, req.returns_10d,
    ]
    X = np.array(feature_values, dtype=np.float64).reshape(1, -1)
    proba      = model.predict_proba(X)[0]
    pred_class = int(np.argmax(proba))
    signal     = le.inverse_transform([pred_class])[0]
    confidence = float(round(proba[pred_class], 4))

    return PredictResponse(
        signal        = signal,
        confidence    = confidence,
        vol_regime    = "low_medium",
        volatility    = req.volatility,
        vol_threshold = vol_threshold,
        note          = (
            "LightGBM model (RELIANCE.NS 2016-2024). Walk-forward avg acc ~51.7%. "
            "Low/medium-vol regime — ~5pp edge above baseline in backtests. "
            "5-day forward direction. NOT investment advice."
        ),
        timestamp     = ts,
    )


@app.post("/compute-and-predict", response_model=MlSignalResponse)
async def compute_and_predict(req: ComputeAndPredictRequest):
    """
    Option B integration endpoint for Android.

    Android sends the raw OHLCV candle array it already fetches from Yahoo Finance.
    Server computes all 14 TA features internally, applies the volatility gate,
    and returns the ML direction signal as a secondary badge alongside Groq output.

    Recommended: send 100+ candles (1-year daily history). Minimum: 30 candles.
    Candles must be ordered oldest → newest.
    """
    if _bundle is None or _threshold is None:
        raise HTTPException(status_code=503, detail="Model not loaded.")

    ts            = datetime.now(timezone.utc).isoformat()
    vol_threshold = _threshold["vol_threshold_q2"]

    # Compute features from raw OHLCV
    features = _compute_features_from_candles(req.candles)
    if features is None:
        return MlSignalResponse(
            symbol        = req.symbol,
            signal        = "INSUFFICIENT_DATA",
            confidence    = None,
            vol_regime    = "unknown",
            volatility    = None,
            vol_threshold = vol_threshold,
            candles_used  = len(req.candles),
            note          = (
                f"Need ≥27 candles for indicator warm-up; received {len(req.candles)}. "
                "Send more historical data (100 daily candles recommended)."
            ),
            timestamp     = ts,
        )

    vol = features["volatility"]

    # Volatility gate
    if vol > vol_threshold:
        return MlSignalResponse(
            symbol        = req.symbol,
            signal        = "NO_RELIABLE_SIGNAL",
            confidence    = None,
            vol_regime    = "high",
            volatility    = round(vol, 6),
            vol_threshold = vol_threshold,
            candles_used  = len(req.candles),
            note          = (
                f"High-volatility regime (vol={vol:.5f} > threshold={vol_threshold:.5f}). "
                "Technical indicators unreliable during sharp moves. No signal emitted."
            ),
            timestamp     = ts,
        )

    # Run model
    model = _bundle["model"]
    le    = _bundle["label_encoder"]
    X     = np.array([features[c] for c in _feature_cols], dtype=np.float64).reshape(1, -1)
    proba      = model.predict_proba(X)[0]
    pred_class = int(np.argmax(proba))
    signal     = le.inverse_transform([pred_class])[0]   # "UP" or "NOT_UP"
    confidence = float(round(proba[pred_class], 4))

    return MlSignalResponse(
        symbol        = req.symbol,
        signal        = signal,
        confidence    = confidence,
        vol_regime    = "low_medium",
        volatility    = round(vol, 6),
        vol_threshold = vol_threshold,
        candles_used  = len(req.candles),
        note          = (
            "LightGBM model (RELIANCE.NS 2016-2024). Walk-forward avg acc ~51.7%. "
            "Low/medium-vol regime — ~5pp edge above baseline in backtests. "
            "5-day forward direction. NOT investment advice."
        ),
        timestamp     = ts,
    )


# ── Run directly ─────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=False)
