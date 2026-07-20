package com.pawan.nextpredict.domain.predictor

import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.PredictionDirection
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rule-based 5-minute price predictor using multi-indicator technical analysis.
 *
 * Combines RSI, MACD, EMA crossover, momentum, Bollinger Bands, volume bias,
 * price structure, and candlestick patterns into a weighted score to forecast
 * the next 5-minute candle direction and price range.
 */
object CandlePredictorEngine {

    private const val MIN_CANDLES = 30
    private const val HORIZON_MINUTES = 5

    data class PredictorOutput(
        val direction: PredictionDirection,
        val confidence: Int,
        val targetPrice: Double,
        val targetLow: Double,
        val targetHigh: Double,
        val reasoning: String,
        val targetTime: String,
    )

    private data class Signal(
        val name: String,
        val score: Double,   // -1.0 (bearish) to +1.0 (bullish)
        val weight: Double,
        val detail: String,
    )

    /**
     * @param intradayCandles 1-min (or 5-min) candles for short-term prediction
     * @param dailyCandles    Daily candles for trend context filter
     * @param currentPrice    Live quote price (may differ from last candle close)
     */
    fun predict(
        intradayCandles: List<HistoricalDataPoint>,
        dailyCandles: List<HistoricalDataPoint>,
        currentPrice: Double,
        isIntraday: Boolean,
    ): PredictorOutput {
        val candles = intradayCandles.ifEmpty { dailyCandles }
        if (candles.size < MIN_CANDLES) {
            return insufficientDataOutput(currentPrice, candles.size, isIntraday)
        }

        val closes = TechnicalIndicators.closes(candles)
        val price = if (currentPrice > 0) currentPrice else closes.last()

        val signals = buildList {
            add(emaCrossSignal(closes))
            add(rsiSignal(closes))
            add(macdSignal(closes))
            add(momentumSignal(closes))
            add(bollingerSignal(closes, price))
            add(priceStructureSignal(candles))
            add(volumeSignal(candles))
            add(candlePatternSignal(candles))
            add(vwapSignal(candles, price))
            if (dailyCandles.size >= 20) add(dailyTrendFilter(dailyCandles))
        }

        val totalWeight = signals.sumOf { it.weight }
        val weightedScore = signals.sumOf { it.score * it.weight } / totalWeight

        val bullishCount = signals.count { it.score > 0.15 }
        val bearishCount = signals.count { it.score < -0.15 }
        val agreement = maxOf(bullishCount, bearishCount)

        val direction = when {
            weightedScore > 0.12 -> PredictionDirection.UP
            weightedScore < -0.12 -> PredictionDirection.DOWN
            else -> PredictionDirection.SIDEWAYS
        }

        val baseConfidence = (50 + abs(weightedScore) * 40 + agreement * 3).coerceIn(45.0, 92.0)
        val confidence = when (direction) {
            PredictionDirection.SIDEWAYS -> (baseConfidence * 0.75).roundToInt().coerceIn(40, 65)
            else -> baseConfidence.roundToInt()
        }

        val atr = TechnicalIndicators.atr(candles, 14)
            ?: TechnicalIndicators.avgCandleRange(candles, 5)
        val fiveMinMove = estimateFiveMinuteMove(candles, atr, direction, weightedScore)

        val (targetPrice, targetLow, targetHigh) = computeTargets(
            price = price,
            direction = direction,
            expectedMove = fiveMinMove,
            atr = atr,
            isIntraday = isIntraday,
        )

        val reasoning = buildReasoning(signals, weightedScore, direction, price, targetPrice, atr)

        val targetTime = if (isIntraday) "Next $HORIZON_MINUTES Minutes" else "Next Trading Day"

        return PredictorOutput(
            direction = direction,
            confidence = confidence,
            targetPrice = targetPrice,
            targetLow = targetLow,
            targetHigh = targetHigh,
            reasoning = reasoning,
            targetTime = targetTime,
        )
    }

    // ── Signal builders ──────────────────────────────────────────────────────

    private fun emaCrossSignal(closes: List<Double>): Signal {
        val ema9 = TechnicalIndicators.ema(closes, 9)
        val ema21 = TechnicalIndicators.ema(closes, 21)
        if (ema9 == null || ema21 == null) {
            return Signal("EMA Cross", 0.0, 0.15, "Insufficient data for EMA")
        }
        val gap = (ema9 - ema21) / ema21 * 100
        val score = when {
            gap > 0.15 -> 0.8
            gap > 0.05 -> 0.5
            gap < -0.15 -> -0.8
            gap < -0.05 -> -0.5
            else -> 0.0
        }
        val trend = if (ema9 > ema21) "bullish (EMA9 > EMA21)" else "bearish (EMA9 < EMA21)"
        return Signal("EMA Cross", score, 0.15, "EMA9=${fmt(ema9)}, EMA21=${fmt(ema21)} → $trend")
    }

    private fun rsiSignal(closes: List<Double>): Signal {
        val rsi = TechnicalIndicators.rsi(closes, 14) ?: return Signal("RSI", 0.0, 0.15, "RSI unavailable")
        val score = when {
            rsi < 30 -> 0.85          // oversold → bounce expected
            rsi < 40 -> 0.4
            rsi > 70 -> -0.85         // overbought → pullback expected
            rsi > 60 -> -0.4
            rsi in 50.0..60.0 -> 0.25 // mild bullish momentum
            rsi in 40.0..50.0 -> -0.25
            else -> 0.0
        }
        val zone = when {
            rsi < 30 -> "oversold"
            rsi > 70 -> "overbought"
            rsi in 45.0..55.0 -> "neutral"
            rsi > 55 -> "bullish momentum"
            else -> "bearish momentum"
        }
        return Signal("RSI(14)", score, 0.15, "RSI=${"%.1f".format(rsi)} ($zone)")
    }

    private fun macdSignal(closes: List<Double>): Signal {
        val macd = TechnicalIndicators.macd(closes) ?: return Signal("MACD", 0.0, 0.15, "MACD unavailable")
        val crossBullish = macd.macdLine > macd.signalLine && macd.histogram > macd.prevHistogram
        val crossBearish = macd.macdLine < macd.signalLine && macd.histogram < macd.prevHistogram
        val score = when {
            crossBullish && macd.histogram > 0 -> 0.85
            macd.histogram > 0 && macd.histogram > macd.prevHistogram -> 0.5
            crossBearish && macd.histogram < 0 -> -0.85
            macd.histogram < 0 && macd.histogram < macd.prevHistogram -> -0.5
            macd.histogram > 0 -> 0.2
            macd.histogram < 0 -> -0.2
            else -> 0.0
        }
        val trend = if (macd.histogram > macd.prevHistogram) "strengthening" else "weakening"
        return Signal(
            "MACD",
            score,
            0.15,
            "Histogram=${fmt(macd.histogram)} ($trend), MACD ${if (macd.macdLine > macd.signalLine) "above" else "below"} signal",
        )
    }

    private fun momentumSignal(closes: List<Double>): Signal {
        val roc5 = TechnicalIndicators.roc(closes, 5) ?: return Signal("Momentum", 0.0, 0.12, "ROC unavailable")
        val roc3 = TechnicalIndicators.roc(closes, 3) ?: roc5
        val score = when {
            roc5 > 0.3 && roc3 > 0 -> 0.75
            roc5 > 0.1 -> 0.4
            roc5 < -0.3 && roc3 < 0 -> -0.75
            roc5 < -0.1 -> -0.4
            else -> 0.0
        }
        return Signal("Momentum", score, 0.12, "ROC(5)=${"%.2f".format(roc5)}%, ROC(3)=${"%.2f".format(roc3)}%")
    }

    private fun bollingerSignal(closes: List<Double>, price: Double): Signal {
        val bb = TechnicalIndicators.bollingerBands(closes) ?: return Signal("Bollinger", 0.0, 0.08, "BB unavailable")
        val score = when {
            bb.percentB < 0.1 -> 0.7    // at lower band → bounce
            bb.percentB < 0.25 -> 0.35
            bb.percentB > 0.9 -> -0.7   // at upper band → pullback
            bb.percentB > 0.75 -> -0.35
            bb.percentB > 0.5 -> 0.15
            else -> -0.15
        }
        return Signal(
            "Bollinger",
            score,
            0.08,
            "%B=${"%.2f".format(bb.percentB)} (Upper=${fmt(bb.upper)}, Lower=${fmt(bb.lower)})",
        )
    }

    private fun priceStructureSignal(candles: List<HistoricalDataPoint>): Signal {
        val structure = TechnicalIndicators.priceStructure(candles, 5)
        val score = when (structure) {
            1 -> 0.8
            -1 -> -0.8
            else -> 0.0
        }
        val label = when (structure) {
            1 -> "higher highs & higher lows"
            -1 -> "lower highs & lower lows"
            else -> "mixed / consolidating"
        }
        return Signal("Price Structure", score, 0.15, "Last 5 candles: $label")
    }

    private fun volumeSignal(candles: List<HistoricalDataPoint>): Signal {
        val bias = TechnicalIndicators.volumeBias(candles)
        val score = when (bias) {
            1 -> 0.6
            -1 -> -0.6
            else -> 0.0
        }
        val label = when (bias) {
            1 -> "buyers dominating (higher vol on green candles)"
            -1 -> "sellers dominating (higher vol on red candles)"
            else -> "balanced volume"
        }
        return Signal("Volume", score, 0.10, label)
    }

    private fun candlePatternSignal(candles: List<HistoricalDataPoint>): Signal {
        val pattern = TechnicalIndicators.detectPattern(candles)
        val score = when (pattern) {
            TechnicalIndicators.CandlePattern.BULLISH_ENGULFING -> 0.9
            TechnicalIndicators.CandlePattern.HAMMER -> 0.7
            TechnicalIndicators.CandlePattern.BEARISH_ENGULFING -> -0.9
            TechnicalIndicators.CandlePattern.SHOOTING_STAR -> -0.7
            TechnicalIndicators.CandlePattern.DOJI -> 0.0
            TechnicalIndicators.CandlePattern.NONE -> 0.0
        }
        val label = pattern.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        return Signal("Candle Pattern", score, 0.10, if (pattern == TechnicalIndicators.CandlePattern.NONE) "No strong pattern" else label)
    }

    private fun vwapSignal(candles: List<HistoricalDataPoint>, price: Double): Signal {
        val vwap = TechnicalIndicators.vwap(candles) ?: return Signal("VWAP", 0.0, 0.10, "VWAP unavailable")
        val deviation = (price - vwap) / vwap * 100
        val score = when {
            deviation > 0.3 -> 0.5    // above VWAP → bullish
            deviation > 0.1 -> 0.25
            deviation < -0.3 -> -0.5
            deviation < -0.1 -> -0.25
            else -> 0.0
        }
        return Signal("VWAP", score, 0.10, "Price ${if (deviation >= 0) "above" else "below"} VWAP by ${"%.2f".format(abs(deviation))}%")
    }

    private fun dailyTrendFilter(dailyCandles: List<HistoricalDataPoint>): Signal {
        val closes = TechnicalIndicators.closes(dailyCandles)
        val ema20 = TechnicalIndicators.ema(closes, 20)
        val ema50 = TechnicalIndicators.ema(closes, 50)
        if (ema20 == null || ema50 == null) return Signal("Daily Trend", 0.0, 0.10, "Daily trend N/A")

        val dailyRsi = TechnicalIndicators.rsi(closes, 14)
        val score = when {
            ema20 > ema50 && (dailyRsi ?: 50.0) > 45 -> 0.6
            ema20 < ema50 && (dailyRsi ?: 50.0) < 55 -> -0.6
            ema20 > ema50 -> 0.3
            else -> -0.3
        }
        return Signal(
            "Daily Trend",
            score,
            0.10,
            "Daily EMA20 ${if (ema20 > ema50) ">" else "<"} EMA50" +
                (dailyRsi?.let { ", RSI=${"%.1f".format(it)}" } ?: ""),
        )
    }

    // ── Price target computation ─────────────────────────────────────────────

    private fun estimateFiveMinuteMove(
        candles: List<HistoricalDataPoint>,
        atr: Double,
        direction: PredictionDirection,
        score: Double,
    ): Double {
        val avgRange = TechnicalIndicators.avgCandleRange(candles, 5)
        // Scale 1-min ATR to 5-minute expected move
        val baseMove = maxOf(atr * 2.2, avgRange * 1.5)
        val strength = abs(score).coerceIn(0.1, 1.0)
        val signedMove = when (direction) {
            PredictionDirection.UP -> baseMove * strength
            PredictionDirection.DOWN -> -baseMove * strength
            PredictionDirection.SIDEWAYS -> 0.0
        }
        return signedMove
    }

    private fun computeTargets(
        price: Double,
        direction: PredictionDirection,
        expectedMove: Double,
        atr: Double,
        isIntraday: Boolean,
    ): Triple<Double, Double, Double> {
        val rangeHalf = if (isIntraday) atr * 1.8 else atr * 3.0

        return when (direction) {
            PredictionDirection.UP -> Triple(
                price + abs(expectedMove),
                price,
                price + rangeHalf,
            )
            PredictionDirection.DOWN -> Triple(
                price - abs(expectedMove),
                price - rangeHalf,
                price,
            )
            PredictionDirection.SIDEWAYS -> Triple(
                price,
                price - rangeHalf * 0.5,
                price + rangeHalf * 0.5,
            )
        }
    }

    // ── Reasoning builder ────────────────────────────────────────────────────

    private fun buildReasoning(
        signals: List<Signal>,
        weightedScore: Double,
        direction: PredictionDirection,
        price: Double,
        targetPrice: Double,
        atr: Double,
    ): String = buildString {
        val bullish = signals.filter { it.score > 0.15 }
        val bearish = signals.filter { it.score < -0.15 }

        append("Analysis of ${signals.size} indicators (score: ${"%.2f".format(weightedScore)}):\n\n")

        append("▲ Bullish signals (${bullish.size}):\n")
        if (bullish.isEmpty()) append("  • None strong\n")
        else bullish.forEach { append("  • ${it.name}: ${it.detail}\n") }

        append("\n▼ Bearish signals (${bearish.size}):\n")
        if (bearish.isEmpty()) append("  • None strong\n")
        else bearish.forEach { append("  • ${it.name}: ${it.detail}\n") }

        append("\n📊 Forecast: ")
        append(
            when (direction) {
                PredictionDirection.UP -> "Price likely to rise from ₹${fmt(price)} toward ₹${fmt(targetPrice)}"
                PredictionDirection.DOWN -> "Price likely to fall from ₹${fmt(price)} toward ₹${fmt(targetPrice)}"
                PredictionDirection.SIDEWAYS -> "Price expected to stay near ₹${fmt(price)} with limited movement"
            },
        )
        append(" in the next $HORIZON_MINUTES minutes.\n")
        append("ATR(14)=₹${fmt(atr)} used for range calculation.")
    }

    private fun insufficientDataOutput(price: Double, count: Int, isIntraday: Boolean): PredictorOutput {
        return PredictorOutput(
            direction = PredictionDirection.SIDEWAYS,
            confidence = 30,
            targetPrice = price,
            targetLow = price * 0.999,
            targetHigh = price * 1.001,
            reasoning = "Need at least $MIN_CANDLES candles for reliable analysis (got $count). " +
                "Try again when market is open with more intraday data.",
            targetTime = if (isIntraday) "Next $HORIZON_MINUTES Minutes" else "Next Trading Day",
        )
    }

    private fun fmt(value: Double): String = "%.2f".format(value)
}
