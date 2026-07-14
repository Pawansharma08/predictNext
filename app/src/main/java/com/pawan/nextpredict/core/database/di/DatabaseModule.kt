package com.pawan.nextpredict.core.database.di

import android.content.Context
import androidx.room.Room
import com.pawan.nextpredict.core.database.AppDatabase
import com.pawan.nextpredict.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context = context,
        klass = AppDatabase::class.java,
        name = AppDatabase.DATABASE_NAME,
    )
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()

    @Provides
    @Singleton
    fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    @Singleton
    fun provideWatchlistItemDao(db: AppDatabase): WatchlistItemDao = db.watchlistItemDao()

    @Provides
    @Singleton
    fun provideStockCacheDao(db: AppDatabase): StockCacheDao = db.stockCacheDao()

    @Provides
    @Singleton
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    @Singleton
    fun providePriceAlertDao(db: AppDatabase): PriceAlertDao = db.priceAlertDao()
}
