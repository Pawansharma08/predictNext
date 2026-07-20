package com.pawan.nextpredict.domain.predictor

import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.PredictionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandlePredictorEngineTest {

    @Test
    fun `predict returns insufficient data when candles are too few`() {
        val candles = buildTrendingCandles(count = 10, startPrice = 100.0, step = 0.5)
        val result = CandlePredictorEngine.predict(
            intradayCandles = candles,
            dailyCandles = emptyList(),
            currentPrice = 105.0,
            isIntraday = true,
        )
        assertEquals(PredictionDirection.SIDEWAYS, result.direction)
        assertTrue(result.confidence <= 35)
        assertTrue(result.reasoning.contains("Need at least"))
    }

    @Test
    fun `predict detects bullish trend from rising candles`() {
        val intraday = buildTrendingCandles(count = 60, startPrice = 100.0, step = 0.3)
        val daily = buildTrendingCandles(count = 60, startPrice = 90.0, step = 0.5)
        val result = CandlePredictorEngine.predict(
            intradayCandles = intraday,
            dailyCandles = daily,
            currentPrice = intraday.last().close,
            isIntraday = true,
        )
        assertEquals(PredictionDirection.UP, result.direction)
        assertTrue(result.confidence >= 50)
        assertTrue(result.targetPrice > intraday.last().close)
    }

    @Test
    fun `predict detects bearish trend from falling candles`() {
        val intraday = buildTrendingCandles(count = 60, startPrice = 200.0, step = -0.4)
        val daily = buildTrendingCandles(count = 60, startPrice = 220.0, step = -0.5)
        val result = CandlePredictorEngine.predict(
            intradayCandles = intraday,
            dailyCandles = daily,
            currentPrice = intraday.last().close,
            isIntraday = true,
        )
        assertEquals(PredictionDirection.DOWN, result.direction)
        assertTrue(result.targetPrice < intraday.last().close)
    }

    @Test
    fun `rsi returns oversold for falling then flat prices`() {
        val closes = buildTrendingCandles(30, 100.0, -0.5).map { it.close }
        val rsi = TechnicalIndicators.rsi(closes, 14)
        assertTrue(rsi != null && rsi < 40)
    }

    private fun buildTrendingCandles(count: Int, startPrice: Double, step: Double): List<HistoricalDataPoint> {
        return (0 until count).map { i ->
            val price = startPrice + i * step
            val open = price - step * 0.3
            HistoricalDataPoint(
                date = "2026-01-01 ${"%02d".format(i / 60)}:${"%02d".format(i % 60)}",
                open = open,
                high = price + kotlin.math.abs(step) * 0.5,
                low = price - kotlin.math.abs(step) * 0.5,
                close = price,
                volume = 10_000L + i * 100,
            )
        }
    }
}
