package com.pawan.nextpredict.domain.repository

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.network.NseApiConstants
import com.pawan.nextpredict.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for all market-wide data.
 * Implementation can be swapped to Zerodha, Upstox, etc.
 */
interface MarketRepository {
    suspend fun getMarketStatus(): ApiResult<MarketStatus>
    suspend fun getAllIndices(): ApiResult<List<Index>>
    suspend fun getIndexConstituents(indexName: String): ApiResult<List<Stock>>
    suspend fun getTopGainers(indexName: String = NseApiConstants.Index.NIFTY_50): ApiResult<List<Stock>>
    suspend fun getTopLosers(indexName: String = NseApiConstants.Index.NIFTY_50): ApiResult<List<Stock>>
    suspend fun getMostActive(): ApiResult<List<Stock>>
    suspend fun getPreOpenMarket(key: String = "NIFTY"): ApiResult<List<PreOpenStock>>
}

/**
 * Repository interface for individual stock data.
 */
interface StockRepository {
    suspend fun getStockQuote(symbol: String): ApiResult<StockQuote>
    suspend fun getOptionChain(symbol: String): ApiResult<OptionChain>
    suspend fun getHistoricalData(
        symbol: String,
        series: String,
        fromDate: String,
        toDate: String,
    ): ApiResult<List<HistoricalDataPoint>>
    /**
     * Fetches intraday (minute-level) OHLCV candles from Alpha Vantage.
     * @param interval One of: "1min", "5min", "15min", "30min", "60min"
     */
    suspend fun getIntradayData(
        symbol: String,
        interval: String = "1min",
    ): ApiResult<List<HistoricalDataPoint>>
    suspend fun getCorporateAnnouncements(symbol: String): ApiResult<List<NewsItem>>
}

/**
 * Repository interface for stock search.
 */
interface SearchRepository {
    suspend fun searchStocks(query: String): ApiResult<List<SearchResult>>
    fun getSearchHistory(): Flow<List<SearchResult>>
    suspend fun addToSearchHistory(result: SearchResult)
    suspend fun removeFromSearchHistory(symbol: String)
    suspend fun clearSearchHistory()
}

/**
 * Repository interface for watchlist management (local-only).
 */
interface WatchlistRepository {
    fun getAllWatchlists(): Flow<List<Watchlist>>
    fun getWatchlistItems(watchlistId: Long): Flow<List<WatchlistStock>>
    fun isSymbolInWatchlist(watchlistId: Long, symbol: String): Flow<Boolean>
    suspend fun createWatchlist(name: String): Long
    suspend fun renameWatchlist(watchlistId: Long, newName: String)
    suspend fun deleteWatchlist(watchlistId: Long)
    suspend fun addStockToWatchlist(watchlistId: Long, symbol: String, companyName: String)
    suspend fun removeStockFromWatchlist(watchlistId: Long, symbol: String)
    suspend fun reorderWatchlistItem(watchlistId: Long, symbol: String, newOrder: Int)
}

/**
 * Repository interface for price alerts (local-only, checked by WorkManager).
 */
interface AlertRepository {
    fun getAllAlerts(): Flow<List<PriceAlert>>
    suspend fun getActiveAlerts(): List<PriceAlert>
    suspend fun createAlert(
        symbol: String,
        companyName: String,
        targetPrice: Double,
        condition: AlertCondition,
    ): Long
    suspend fun deleteAlert(alertId: Long)
    suspend fun markAlertAsTriggered(alertId: Long)
    suspend fun toggleAlert(alertId: Long, isActive: Boolean)
}

/**
 * Repository interface for AI-powered price predictions via Grok.
 */
interface PredictionRepository {
    suspend fun predictNextPrice(
        symbol: String,
        currentPrice: Double,
        history: List<com.pawan.nextpredict.domain.model.HistoricalDataPoint>,
        isIntraday: Boolean = false,
        fiveYearSummary: String = "",
    ): com.pawan.nextpredict.core.common.ApiResult<com.pawan.nextpredict.domain.model.PricePrediction>
}
