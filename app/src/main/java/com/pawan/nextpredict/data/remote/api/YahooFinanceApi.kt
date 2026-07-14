package com.pawan.nextpredict.data.remote.api

import com.pawan.nextpredict.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YahooFinanceApi {



    /**
     * Get historical or intraday chart data points (OHLCV).
     * @param symbol Stock ticker, e.g. "RELIANCE.NS" or "AAPL"
     * @param interval Interval between candles (e.g. "1m", "5m", "1d", "1wk")
     * @param range Range of data to return (e.g. "1d", "5d", "1mo", "3mo", "1y", "5y")
     */
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChartData(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("range") range: String
    ): YahooChartResponseDto

    /**
     * Autocomplete / search stock tickers based on query.
     */
    @GET("v1/finance/search")
    suspend fun searchStocks(
        @Query("q") query: String
    ): YahooSearchResponseDto
}
