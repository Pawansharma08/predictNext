package com.pawan.nextpredict.data.repository.di

import com.pawan.nextpredict.data.repository.*
import com.pawan.nextpredict.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMarketRepository(
        impl: MarketRepositoryImpl,
    ): MarketRepository

    @Binds
    @Singleton
    abstract fun bindStockRepository(
        impl: StockRepositoryImpl,
    ): StockRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl,
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(
        impl: WatchlistRepositoryImpl,
    ): WatchlistRepository
}
