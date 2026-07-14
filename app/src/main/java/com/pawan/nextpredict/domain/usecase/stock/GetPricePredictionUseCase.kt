package com.pawan.nextpredict.domain.usecase.stock

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.PricePrediction
import com.pawan.nextpredict.domain.repository.PredictionRepository
import com.pawan.nextpredict.domain.repository.StockRepository
import javax.inject.Inject
import kotlin.math.abs

/**
 * Fetches 5 years of daily OHLCV history + optional intraday candles,
 * builds a rich multi-timeframe context, then asks the Groq AI to predict
 * the next 5-minute price movement.
 */
class GetPricePredictionUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val predictionRepository: PredictionRepository,
) {
    companion object {
        const val INTRADAY_INTERVAL    = "1min"
        /** Recent daily candles sent verbatim to the AI. */
        const val RECENT_CANDLES       = 100
        /** Maximum trading days in compact outputsize (100 days / ~5 months). */
        const val MAX_FREE_DAYS        = 100
        /** Half of the historical context (~50 days). */
        const val MID_TERM_DAYS        = 50
    }

    suspend operator fun invoke(
        symbol: String,
        currentPrice: Double,
        dailyHistory: List<HistoricalDataPoint>,
    ): ApiResult<PricePrediction> {

        // Slice to maximum available free history from pre-fetched dailyHistory
        val hundredDays = dailyHistory.takeLast(MAX_FREE_DAYS)
        val midTerm     = dailyHistory.takeLast(MID_TERM_DAYS)

        // ── Step 1: Compute 100-day summary statistics ───────────────────────────
        val historySummary = buildHistorySummary(hundredDays, midTerm, currentPrice)

        // ── Step 2: Try to get 1-minute intraday candles for recent momentum ─────
        val intradayResult = stockRepository.getIntradayData(symbol, INTRADAY_INTERVAL)
        val isIntraday = intradayResult is ApiResult.Success && intradayResult.data.isNotEmpty()

        // Use intraday for the "recent" candles if available; else use last 30 daily
        val recentCandles: List<HistoricalDataPoint> = if (isIntraday) {
            (intradayResult as ApiResult.Success).data.takeLast(30)
        } else {
            hundredDays.takeLast(RECENT_CANDLES)
        }

        // ── Step 3: Call AI with full multi-timeframe context ─────────────────────
        return predictionRepository.predictNextPrice(
            symbol           = symbol,
            currentPrice     = currentPrice,
            history          = recentCandles,
            isIntraday       = isIntraday,
            fiveYearSummary  = historySummary,
        )
    }

    /**
     * Computes a compact multi-timeframe summary string from historical data.
     * This gives the AI trend and support/resistance context within the 100-day free tier limit.
     */
    private fun buildHistorySummary(
        hundredDays: List<HistoricalDataPoint>,
        midTerm: List<HistoricalDataPoint>,
        currentPrice: Double,
    ): String {
        if (hundredDays.isEmpty()) return "No historical daily data available."

        val high100d  = hundredDays.maxOf { it.high }
        val low100d   = hundredDays.minOf { it.low }
        val avg100d   = hundredDays.map { it.close }.average()

        val high50d   = if (midTerm.isNotEmpty()) midTerm.maxOf { it.high } else high100d
        val low50d    = if (midTerm.isNotEmpty()) midTerm.minOf { it.low }  else low100d
        val avg50d    = if (midTerm.isNotEmpty()) midTerm.map { it.close }.average() else avg100d

        // Simple 100-day momentum
        val price100dAgo  = hundredDays.getOrNull(0)?.close ?: hundredDays.first().close
        val momentum100d  = ((currentPrice - price100dAgo) / price100dAgo * 100)
        val momentumStr  = if (momentum100d >= 0) "+%.1f%%".format(momentum100d)
                           else "%.1f%%".format(momentum100d)

        // Trend: compare 50-day avg vs 100-day avg
        val trend = when {
            avg50d > avg100d * 1.03  -> "Strong uptrend (50D avg above 100D avg)"
            avg50d > avg100d * 1.01  -> "Mild uptrend"
            avg50d < avg100d * 0.97  -> "Strong downtrend (50D avg below 100D avg)"
            avg50d < avg100d * 0.99  -> "Mild downtrend"
            else                     -> "Sideways/consolidation"
        }

        // Distance from 100D high/low as support/resistance context
        val distFromHigh = "%.1f%%".format((high100d - currentPrice) / currentPrice * 100)
        val distFromLow  = "%.1f%%".format((currentPrice - low100d) / currentPrice * 100)

        val compareStr = if (currentPrice > avg100d) "ABOVE" else "BELOW"

        return """
            === 100-DAY CONTEXT (${hundredDays.size} trading days) ===
            100D High: ${"%.2f".format(high100d)} | 100D Low: ${"%.2f".format(low100d)} | 100D Avg Close: ${"%.2f".format(avg100d)}
            50D High: ${"%.2f".format(high50d)} | 50D Low: ${"%.2f".format(low50d)} | 50D Avg Close: ${"%.2f".format(avg50d)}
            100-Day Momentum: $momentumStr
            Mid-Term Trend: $trend
            Distance from 100D High: $distFromHigh (resistance)
            Distance from 100D Low: $distFromLow (support floor)
            Current vs 100D Avg: $compareStr mid-term average
        """.trimIndent()
    }

}


