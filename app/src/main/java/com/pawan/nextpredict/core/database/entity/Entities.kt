package com.pawan.nextpredict.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a watchlist group (e.g., "My Watchlist", "Swing Trades").
 */
@Entity(tableName = "watchlists")
data class WatchlistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Room entity representing a stock symbol in a watchlist.
 */
@Entity(
    tableName = "watchlist_items",
    primaryKeys = ["watchlist_id", "symbol"],
)
data class WatchlistItemEntity(
    @ColumnInfo(name = "watchlist_id")
    val watchlistId: Long,

    @ColumnInfo(name = "symbol")
    val symbol: String,

    @ColumnInfo(name = "company_name")
    val companyName: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * Room entity for caching recent stock quote lookups.
 */
@Entity(tableName = "stock_cache")
data class StockCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "symbol")
    val symbol: String,

    @ColumnInfo(name = "company_name")
    val companyName: String,

    @ColumnInfo(name = "last_price")
    val lastPrice: Double,

    @ColumnInfo(name = "change")
    val change: Double,

    @ColumnInfo(name = "change_percent")
    val changePercent: Double,

    @ColumnInfo(name = "open")
    val open: Double,

    @ColumnInfo(name = "high")
    val high: Double,

    @ColumnInfo(name = "low")
    val low: Double,

    @ColumnInfo(name = "close")
    val close: Double,

    @ColumnInfo(name = "volume")
    val volume: Long,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis(),
)

/**
 * Room entity for search history.
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "symbol")
    val symbol: String,

    @ColumnInfo(name = "company_name")
    val companyName: String,

    @ColumnInfo(name = "searched_at")
    val searchedAt: Long = System.currentTimeMillis(),
)

/**
 * Room entity for price alerts set by the user.
 */
@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "symbol")
    val symbol: String,

    @ColumnInfo(name = "company_name")
    val companyName: String,

    @ColumnInfo(name = "target_price")
    val targetPrice: Double,

    @ColumnInfo(name = "condition") // "ABOVE" or "BELOW"
    val condition: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "triggered_at")
    val triggeredAt: Long? = null,
)
