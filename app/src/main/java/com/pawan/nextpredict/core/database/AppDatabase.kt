package com.pawan.nextpredict.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pawan.nextpredict.core.database.dao.*
import com.pawan.nextpredict.core.database.entity.*

@Database(
    entities = [
        WatchlistEntity::class,
        WatchlistItemEntity::class,
        StockCacheEntity::class,
        SearchHistoryEntity::class,
        PriceAlertEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchlistItemDao(): WatchlistItemDao
    abstract fun stockCacheDao(): StockCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun priceAlertDao(): PriceAlertDao

    companion object {
        const val DATABASE_NAME = "nextpredict.db"
    }
}
