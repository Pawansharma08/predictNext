package com.pawan.nextpredict.core.database.dao

import androidx.room.*
import com.pawan.nextpredict.core.database.entity.*
import kotlinx.coroutines.flow.Flow

// ─── Watchlist ────────────────────────────────────────────────────────────────

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlists ORDER BY sort_order ASC")
    fun getAllWatchlists(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlists WHERE id = :id")
    suspend fun getWatchlistById(id: Long): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(watchlist: WatchlistEntity): Long

    @Update
    suspend fun updateWatchlist(watchlist: WatchlistEntity)

    @Delete
    suspend fun deleteWatchlist(watchlist: WatchlistEntity)

    @Query("DELETE FROM watchlist_items WHERE watchlist_id = :watchlistId")
    suspend fun clearWatchlistItems(watchlistId: Long)
}

@Dao
interface WatchlistItemDao {

    @Query(
        """
        SELECT * FROM watchlist_items 
        WHERE watchlist_id = :watchlistId 
        ORDER BY sort_order ASC
        """
    )
    fun getItemsForWatchlist(watchlistId: Long): Flow<List<WatchlistItemEntity>>

    @Query(
        """
        SELECT watchlist_id FROM watchlist_items WHERE symbol = :symbol
        """
    )
    suspend fun getWatchlistIdsForSymbol(symbol: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WatchlistItemEntity)

    @Query("DELETE FROM watchlist_items WHERE watchlist_id = :watchlistId AND symbol = :symbol")
    suspend fun deleteItem(watchlistId: Long, symbol: String)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM watchlist_items 
            WHERE watchlist_id = :watchlistId AND symbol = :symbol
        )
        """
    )
    fun isSymbolInWatchlist(watchlistId: Long, symbol: String): Flow<Boolean>
}

// ─── Stock Cache ──────────────────────────────────────────────────────────────

@Dao
interface StockCacheDao {

    @Query("SELECT * FROM stock_cache WHERE symbol = :symbol")
    suspend fun getCachedStock(symbol: String): StockCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stock: StockCacheEntity)

    @Query("DELETE FROM stock_cache WHERE cached_at < :expiryTimestamp")
    suspend fun deleteExpiredCache(expiryTimestamp: Long)

    @Query("DELETE FROM stock_cache")
    suspend fun clearAll()
}

// ─── Search History ───────────────────────────────────────────────────────────

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searched_at DESC LIMIT 20")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE symbol = :symbol")
    suspend fun deleteFromHistory(symbol: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAllHistory()
}

// ─── Price Alerts ─────────────────────────────────────────────────────────────

@Dao
interface PriceAlertDao {

    @Query("SELECT * FROM price_alerts ORDER BY created_at DESC")
    fun getAllAlerts(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE is_active = 1")
    suspend fun getActiveAlerts(): List<PriceAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlertEntity): Long

    @Update
    suspend fun updateAlert(alert: PriceAlertEntity)

    @Delete
    suspend fun deleteAlert(alert: PriceAlertEntity)

    @Query("DELETE FROM price_alerts WHERE id = :alertId")
    suspend fun deleteAlertById(alertId: Long)
}
