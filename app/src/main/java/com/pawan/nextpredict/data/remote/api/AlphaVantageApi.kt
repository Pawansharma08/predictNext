package com.pawan.nextpredict.data.remote.api

import com.pawan.nextpredict.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Alpha Vantage REST API definition.
 * Apikey query parameter is automatically appended via OkHttp interceptor.
 */
interface AlphaVantageApi {

    /**
     * Get global market open & close status.
     */
    @GET("query")
    suspend fun getMarketStatus(
        @Query("function") function: String = "MARKET_STATUS"
    ): MarketStatusResponseDto

    /**
     * Get real-time price & volume quotes for a single equity.
     */
    @GET("query")
    suspend fun getGlobalQuote(
        @Query("symbol") symbol: String,
        @Query("function") function: String = "GLOBAL_QUOTE"
    ): GlobalQuoteResponseDto

    /**
     * Search stock symbols and companies based on keyword match.
     */
    @GET("query")
    suspend fun searchStocks(
        @Query("keywords") keywords: String,
        @Query("function") function: String = "SYMBOL_SEARCH"
    ): SearchResponseDto

    /**
     * Get top gainers, losers, and most active stocks in the US market.
     */
    @GET("query")
    suspend fun getTopGainersLosers(
        @Query("function") function: String = "TOP_GAINERS_LOSERS"
    ): TopMoversResponseDto

    /**
     * Get daily historical time series data (OHLCV).
     */
    @GET("query")
    suspend fun getTimeSeriesDaily(
        @Query("symbol") symbol: String,
        @Query("outputsize") outputSize: String = "compact",
        @Query("function") function: String = "TIME_SERIES_DAILY"
    ): TimeSeriesDailyResponseDto

    /**
     * Get intraday (minute-level) OHLCV candles.
     * Supported intervals: "1min", "5min", "15min", "30min", "60min".
     * outputsize: "compact" = last 100 data points, "full" = up to 30 days.
     * NOTE: Only available for US-listed equities on Alpha Vantage free tier.
     */
    @GET("query")
    suspend fun getTimeSeriesIntraday(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1min",
        @Query("outputsize") outputSize: String = "compact",
        @Query("function") function: String = "TIME_SERIES_INTRADAY"
    ): IntradayResponseDto
}
