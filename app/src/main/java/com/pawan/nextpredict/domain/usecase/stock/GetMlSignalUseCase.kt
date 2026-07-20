package com.pawan.nextpredict.domain.usecase.stock

import com.pawan.nextpredict.data.remote.api.MlPredictionApi
import com.pawan.nextpredict.data.remote.dto.OhlcvCandleDto
import com.pawan.nextpredict.data.remote.dto.MlPredictRequestDto
import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.MlSignal
import javax.inject.Inject

/**
 * Calls the local FastAPI LightGBM server via POST /compute-and-predict.
 *
 * Sends the OHLCV candles the app already has (from Yahoo Finance) so the
 * server can compute features and return the volatility-gated ML signal.
 *
 * This is the Option B integration — runs in parallel with Groq, displayed
 * as a secondary ML badge on the StockDetailScreen.
 *
 * Failures are non-fatal: the ViewModel wraps this call in runCatching so
 * Groq's prediction is never blocked or broken if the ML server is down.
 */
class GetMlSignalUseCase @Inject constructor(
    private val mlApi: MlPredictionApi,
) {
    /**
     * @param symbol NSE ticker (e.g. "RELIANCE.NS")
     * @param dailyHistory OHLCV candles already cached by the ViewModel.
     *                     Ordered oldest → newest. Needs 30+ for indicators.
     * @return [MlSignal] domain model, or throws on network error.
     */
    suspend operator fun invoke(
        symbol: String,
        dailyHistory: List<HistoricalDataPoint>,
    ): MlSignal {
        val candles = dailyHistory.map { pt ->
            OhlcvCandleDto(
                date   = pt.date,
                open   = pt.open,
                high   = pt.high,
                low    = pt.low,
                close  = pt.close,
                volume = pt.volume,
            )
        }

        val response = mlApi.computeAndPredict(
            MlPredictRequestDto(symbol = symbol, candles = candles)
        )
        return response.toDomain()
    }
}
