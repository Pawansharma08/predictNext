package com.pawan.nextpredict.domain.usecase.stock

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.PricePrediction
import com.pawan.nextpredict.domain.repository.PredictionRepository
import com.pawan.nextpredict.domain.repository.StockRepository
import javax.inject.Inject

/**
 * Fetches intraday + daily OHLCV candles, then runs the on-device technical
 * analysis engine to predict the next 5-minute price movement.
 */
class GetPricePredictionUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val predictionRepository: PredictionRepository,
) {
    companion object {
        const val INTRADAY_INTERVAL = "1min"
        /** Minimum candles needed for indicator warm-up (RSI, MACD, EMA). */
        const val INTRADAY_LIMIT = 100
        /** Daily candles used for trend context filter. */
        const val DAILY_CONTEXT_LIMIT = 60
    }

    suspend operator fun invoke(
        symbol: String,
        companyName: String,
        currentPrice: Double,
        change: Double,
        changePercent: Double,
        dailyHistory: List<HistoricalDataPoint>,
    ): ApiResult<PricePrediction> {

        // Ensure we have enough daily history for the trend filter
        var historyPoints = dailyHistory
        if (historyPoints.size < DAILY_CONTEXT_LIMIT) {
            val dailyResult = stockRepository.getYahooChartData(symbol, interval = "1d", range = "1y")
            if (dailyResult is ApiResult.Success && dailyResult.data.isNotEmpty()) {
                historyPoints = dailyResult.data
            }
        }

        // Fetch 1-minute intraday candles for short-term prediction
        val intradayResult = stockRepository.getIntradayData(symbol, INTRADAY_INTERVAL)
        val isIntraday = intradayResult is ApiResult.Success && intradayResult.data.size >= 30
        val intradayCandles = if (intradayResult is ApiResult.Success) {
            intradayResult.data.takeLast(INTRADAY_LIMIT)
        } else {
            emptyList()
        }

        return predictionRepository.predictNextPrice(
            symbol = symbol,
            companyName = companyName,
            currentPrice = currentPrice,
            change = change,
            changePercent = changePercent,
            intradayHistory = intradayCandles,
            dailyHistory = historyPoints.takeLast(DAILY_CONTEXT_LIMIT),
            isIntraday = isIntraday,
            fiveYearSummary = "",
        )
    }
}
