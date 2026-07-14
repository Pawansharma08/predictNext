package com.pawan.nextpredict.data.repository

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.safeApiCall
import com.pawan.nextpredict.data.remote.api.YahooFinanceApi
import com.pawan.nextpredict.data.remote.mapper.*
import com.pawan.nextpredict.domain.model.*
import com.pawan.nextpredict.domain.repository.StockRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: YahooFinanceApi,
) : StockRepository {

    override suspend fun getStockQuote(symbol: String): ApiResult<StockQuote> = safeApiCall {
        // Query the chart endpoint (range=1d) as a quote proxy since direct quotes require auth
        val response = api.getChartData(symbol = symbol, interval = "1d", range = "1d")
        response.toDomainQuote()
    }


    override suspend fun getOptionChain(symbol: String): ApiResult<OptionChain> = safeApiCall {
        val quoteResult = getStockQuote(symbol).getOrNull() 
            ?: throw IllegalStateException("Cannot fetch underlying value for option chain")
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
        // "3mo" range gives ~60 trading days of daily candles, fitting our compact needs
        api.getChartData(symbol = symbol, interval = "1d", range = "3mo").toDomain()
    }

    override suspend fun getIntradayData(
        symbol: String,
        interval: String,
    ): ApiResult<List<HistoricalDataPoint>> = safeApiCall {
        // Convert interval format if needed ("1min" -> "1m", "5min" -> "5m")
        val yahooInterval = when (interval) {
            "1min" -> "1m"
            "5min" -> "5m"
            else -> interval
        }
        api.getChartData(symbol = symbol, interval = yahooInterval, range = "1d").toDomain()
    }

    override suspend fun getYahooChartData(
        symbol: String,
        interval: String,
        range: String,
    ): ApiResult<List<HistoricalDataPoint>> = safeApiCall {
        api.getChartData(symbol = symbol, interval = interval, range = range).toDomain()
    }

    override suspend fun getCorporateAnnouncements(symbol: String): ApiResult<List<NewsItem>> =
        safeApiCall {
            emptyList()
        }
}


