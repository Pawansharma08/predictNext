package com.pawan.nextpredict.data.repository

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.PricePrediction
import com.pawan.nextpredict.domain.predictor.CandlePredictorEngine
import com.pawan.nextpredict.domain.repository.PredictionRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device technical-analysis predictor — no ML server or API required.
 * Uses RSI, MACD, EMA, momentum, Bollinger Bands, volume, and candle patterns
 * to forecast the next 5-minute price movement from past OHLCV candles.
 */
@Singleton
class PredictionRepositoryImpl @Inject constructor() : PredictionRepository {

    override suspend fun predictNextPrice(
        symbol: String,
        companyName: String,
        currentPrice: Double,
        change: Double,
        changePercent: Double,
        intradayHistory: List<HistoricalDataPoint>,
        dailyHistory: List<HistoricalDataPoint>,
        isIntraday: Boolean,
        fiveYearSummary: String,
    ): ApiResult<PricePrediction> {
        val output = CandlePredictorEngine.predict(
            intradayCandles = intradayHistory,
            dailyCandles = dailyHistory,
            currentPrice = currentPrice,
            isIntraday = isIntraday,
        )

        val now = LocalDateTime.now()
        val timestamp = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))

        return ApiResult.Success(
            PricePrediction(
                symbol = symbol,
                direction = output.direction,
                targetPrice = output.targetPrice,
                targetLow = output.targetLow,
                targetHigh = output.targetHigh,
                confidence = output.confidence,
                reasoning = output.reasoning,
                generatedAt = timestamp,
                targetTime = output.targetTime,
            ),
        )
    }
}
