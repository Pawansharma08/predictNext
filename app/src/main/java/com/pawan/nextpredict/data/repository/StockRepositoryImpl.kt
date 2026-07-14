package com.pawan.nextpredict.data.repository

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.safeApiCall
import com.pawan.nextpredict.data.remote.api.AlphaVantageApi
import com.pawan.nextpredict.data.remote.mapper.*
import com.pawan.nextpredict.domain.model.*
import com.pawan.nextpredict.domain.repository.StockRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: AlphaVantageApi,
) : StockRepository {

    override suspend fun getStockQuote(symbol: String): ApiResult<StockQuote> = safeApiCall {
        val response = api.getGlobalQuote(symbol)
        val info = response.information ?: response.note
        if (info != null) {
            throw IllegalStateException("Alpha Vantage API Alert: $info")
        }
        response.toDomain()
    }


    override suspend fun getOptionChain(symbol: String): ApiResult<OptionChain> = safeApiCall {
        val quoteResult = api.getGlobalQuote(symbol).toDomain()
        val underlying = quoteResult.lastPrice
        
        // Generate a mock option chain around the last traded price for UI rendering
        val strikeList = ArrayList<OptionStrike>()
        val baseStrike = (underlying / 10).toInt() * 10.0
        
        for (i in -5..5) {
            val strikePrice = baseStrike + (i * 5.0)
            val isCallITM = strikePrice < underlying
            val isPutITM = strikePrice > underlying
            
            strikeList.add(
                OptionStrike(
                    strikePrice = strikePrice,
                    callData = OptionData(
                        strikePrice = strikePrice,
                        expiryDate = "2026-07-30",
                        openInterest = (1000 - kotlin.math.abs(i) * 150).toLong().coerceAtLeast(10L),
                        changeInOI = (100 - kotlin.math.abs(i) * 15).toLong(),
                        totalTradedVolume = (5000 - kotlin.math.abs(i) * 600).toLong().coerceAtLeast(50L),
                        iv = 18.5 + (i * 0.5),
                        ltp = if (isCallITM) (underlying - strikePrice) + 2.5 else 1.5,
                        change = 0.5,
                        bidQty = 100,
                        bidPrice = 1.4,
                        askQty = 100,
                        askPrice = 1.6,
                        totalBuyQty = 0L,
                        totalSellQty = 0L
                    ),
                    putData = OptionData(
                        strikePrice = strikePrice,
                        expiryDate = "2026-07-30",
                        openInterest = (1000 - kotlin.math.abs(i) * 150).toLong().coerceAtLeast(10L),
                        changeInOI = (100 - kotlin.math.abs(i) * 15).toLong(),
                        totalTradedVolume = (5000 - kotlin.math.abs(i) * 600).toLong().coerceAtLeast(50L),
                        iv = 18.5 - (i * 0.5),
                        ltp = if (isPutITM) (strikePrice - underlying) + 2.5 else 1.5,
                        change = -0.5,
                        bidQty = 100,
                        bidPrice = 1.4,
                        askQty = 100,
                        askPrice = 1.6,
                        totalBuyQty = 0L,
                        totalSellQty = 0L
                    )
                )
            )
        }

        OptionChain(
            symbol = symbol,
            expiryDates = listOf("2026-07-30"),
            selectedExpiry = "2026-07-30",
            underlyingValue = underlying,
            strikePrices = strikeList,
            atm = baseStrike,
            maxCallOI = 5000L,
            maxPutOI = 5000L,
            pcr = 0.95,
            timestamp = "10-Jul-2026"
        )
    }

    override suspend fun getHistoricalData(
        symbol: String,
        series: String,
        fromDate: String,
        toDate: String,
    ): ApiResult<List<HistoricalDataPoint>> = safeApiCall {
        // "compact" returns the last 100 daily data points for free.
        val response = api.getTimeSeriesDaily(symbol, outputSize = "compact")
        val info = response.information ?: response.note
        if (info != null) {
            throw IllegalStateException("Alpha Vantage API Alert: $info")
        }
        response.toDomain()
    }


    /**
     * Fetches 1-minute (or configurable interval) intraday candles.
     * "compact" outputsize returns the last ~100 candles — enough for the
     * AI prediction context (we only need the last 30).
     */
    override suspend fun getIntradayData(
        symbol: String,
        interval: String,
    ): ApiResult<List<HistoricalDataPoint>> = safeApiCall {
        val response = api.getTimeSeriesIntraday(symbol = symbol, interval = interval)
        val info = response.information ?: response.note
        if (info != null) {
            throw IllegalStateException("Alpha Vantage API Alert: $info")
        }
        response.toDomain()
    }


    override suspend fun getCorporateAnnouncements(symbol: String): ApiResult<List<NewsItem>> =
        safeApiCall {
            emptyList()
        }
}
