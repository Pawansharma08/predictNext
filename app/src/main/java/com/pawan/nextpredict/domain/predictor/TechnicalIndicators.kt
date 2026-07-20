package com.pawan.nextpredict.domain.predictor

import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure-Kotlin technical indicator calculations over OHLCV candle lists.
 * All functions expect candles in chronological order (oldest → newest).
 */
object TechnicalIndicators {

    fun closes(candles: List<HistoricalDataPoint>): List<Double> =
        candles.map { it.close }

    fun highs(candles: List<HistoricalDataPoint>): List<Double> =
        candles.map { it.high }

    fun lows(candles: List<HistoricalDataPoint>): List<Double> =
        candles.map { it.low }

    fun volumes(candles: List<HistoricalDataPoint>): List<Long> =
        candles.map { it.volume }

    /** Simple Moving Average for the last [period] closes. Returns null if insufficient data. */
    fun sma(values: List<Double>, period: Int): Double? {
        if (values.size < period) return null
        return values.takeLast(period).average()
    }

    /** Exponential Moving Average series (same length as input; early values use SMA seed). */
    fun emaSeries(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        if (values.size < period) return values.mapIndexed { i, _ -> values.take(i + 1).average() }

        val k = 2.0 / (period + 1)
        val result = DoubleArray(values.size)
        var seed = values.take(period).average()
        result[period - 1] = seed
        for (i in period until values.size) {
            seed = values[i] * k + seed * (1 - k)
            result[i] = seed
        }
        // Fill early values with running SMA
        for (i in 0 until period - 1) {
            result[i] = values.take(i + 1).average()
        }
        return result.toList()
    }

    fun ema(values: List<Double>, period: Int): Double? {
        val series = emaSeries(values, period)
        return series.lastOrNull()
    }

    /** Relative Strength Index (Wilder smoothing). */
    fun rsi(closes: List<Double>, period: Int = 14): Double? {
        if (closes.size < period + 1) return null

        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change >= 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period

        for (i in (period + 1) until closes.size) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    data class MacdResult(
        val macdLine: Double,
        val signalLine: Double,
        val histogram: Double,
        val prevHistogram: Double,
    )

    /** MACD(12, 26, 9) on the last candle. */
    fun macd(closes: List<Double>, fast: Int = 12, slow: Int = 26, signal: Int = 9): MacdResult? {
        if (closes.size < slow + signal) return null

        val fastEma = emaSeries(closes, fast)
        val slowEma = emaSeries(closes, slow)
        val macdLine = fastEma.zip(slowEma) { f, s -> f - s }

        val signalSeries = emaSeries(macdLine, signal)
        val lastMacd = macdLine.last()
        val lastSignal = signalSeries.last()
        val lastHist = lastMacd - lastSignal
        val prevHist = if (macdLine.size >= 2) {
            macdLine[macdLine.size - 2] - signalSeries[signalSeries.size - 2]
        } else lastHist

        return MacdResult(lastMacd, lastSignal, lastHist, prevHist)
    }

    /** Average True Range (Wilder smoothing). */
    fun atr(candles: List<HistoricalDataPoint>, period: Int = 14): Double? {
        if (candles.size < period + 1) return null

        val trueRanges = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val high = candles[i].high
            val low = candles[i].low
            val prevClose = candles[i - 1].close
            trueRanges.add(
                maxOf(high - low, abs(high - prevClose), abs(low - prevClose))
            )
        }

        var atr = trueRanges.take(period).average()
        for (i in period until trueRanges.size) {
            atr = (atr * (period - 1) + trueRanges[i]) / period
        }
        return atr
    }

    data class BollingerBands(
        val upper: Double,
        val middle: Double,
        val lower: Double,
        val percentB: Double,
    )

    fun bollingerBands(closes: List<Double>, period: Int = 20, stdDev: Double = 2.0): BollingerBands? {
        if (closes.size < period) return null
        val window = closes.takeLast(period)
        val middle = window.average()
        val variance = window.map { (it - middle) * (it - middle) }.average()
        val std = sqrt(variance)
        val upper = middle + stdDev * std
        val lower = middle - stdDev * std
        val lastClose = closes.last()
        val percentB = if (upper != lower) (lastClose - lower) / (upper - lower) else 0.5
        return BollingerBands(upper, middle, lower, percentB)
    }

    /** Rate of change over [period] candles (%). */
    fun roc(closes: List<Double>, period: Int = 5): Double? {
        if (closes.size <= period) return null
        val current = closes.last()
        val past = closes[closes.size - period - 1]
        if (past == 0.0) return null
        return ((current - past) / past) * 100.0
    }

    /** Volume-weighted average price over all candles. */
    fun vwap(candles: List<HistoricalDataPoint>): Double? {
        if (candles.isEmpty()) return null
        var cumVol = 0L
        var cumPv = 0.0
        for (c in candles) {
            val typical = (c.high + c.low + c.close) / 3.0
            cumPv += typical * c.volume
            cumVol += c.volume
        }
        return if (cumVol > 0) cumPv / cumVol else candles.last().close
    }

    /** Average body size of last [n] candles. */
    fun avgCandleRange(candles: List<HistoricalDataPoint>, n: Int = 5): Double {
        if (candles.isEmpty()) return 0.0
        return candles.takeLast(n).map { it.high - it.low }.average()
    }

    /** Detect higher-highs / higher-lows structure over last [n] candles. Returns +1 bullish, -1 bearish, 0 neutral. */
    fun priceStructure(candles: List<HistoricalDataPoint>, n: Int = 5): Int {
        if (candles.size < n) return 0
        val recent = candles.takeLast(n)
        var higherHighs = 0
        var lowerLows = 0
        for (i in 1 until recent.size) {
            if (recent[i].high > recent[i - 1].high) higherHighs++
            if (recent[i].low < recent[i - 1].low) lowerLows++
        }
        return when {
            higherHighs >= n - 2 && lowerLows <= 1 -> 1
            lowerLows >= n - 2 && higherHighs <= 1 -> -1
            else -> 0
        }
    }

    /** Volume trend: compares avg volume of last 3 green vs last 3 red candles. +1 bullish, -1 bearish. */
    fun volumeBias(candles: List<HistoricalDataPoint>): Int {
        val green = candles.filter { it.close >= it.open }
        val red = candles.filter { it.close < it.open }
        if (green.size < 2 || red.size < 2) return 0

        val greenVol = green.takeLast(3).map { it.volume }.average()
        val redVol = red.takeLast(3).map { it.volume }.average()
        return when {
            greenVol > redVol * 1.15 -> 1
            redVol > greenVol * 1.15 -> -1
            else -> 0
        }
    }

    enum class CandlePattern { BULLISH_ENGULFING, BEARISH_ENGULFING, HAMMER, SHOOTING_STAR, DOJI, NONE }

    fun detectPattern(candles: List<HistoricalDataPoint>): CandlePattern {
        if (candles.size < 2) return CandlePattern.NONE
        val prev = candles[candles.size - 2]
        val curr = candles.last()

        val prevBody = abs(prev.close - prev.open)
        val currBody = abs(curr.close - curr.open)
        val currRange = curr.high - curr.low
        if (currRange == 0.0) return CandlePattern.NONE

        val upperWick = curr.high - maxOf(curr.open, curr.close)
        val lowerWick = minOf(curr.open, curr.close) - curr.low

        // Doji
        if (currBody / currRange < 0.1) return CandlePattern.DOJI

        // Bullish engulfing
        if (prev.close < prev.open && curr.close > curr.open &&
            curr.open <= prev.close && curr.close >= prev.open && currBody > prevBody
        ) return CandlePattern.BULLISH_ENGULFING

        // Bearish engulfing
        if (prev.close > prev.open && curr.close < curr.open &&
            curr.open >= prev.close && curr.close <= prev.open && currBody > prevBody
        ) return CandlePattern.BEARISH_ENGULFING

        // Hammer (bullish reversal)
        if (lowerWick > currBody * 2 && upperWick < currBody * 0.5) return CandlePattern.HAMMER

        // Shooting star (bearish reversal)
        if (upperWick > currBody * 2 && lowerWick < currBody * 0.5) return CandlePattern.SHOOTING_STAR

        return CandlePattern.NONE
    }
}
