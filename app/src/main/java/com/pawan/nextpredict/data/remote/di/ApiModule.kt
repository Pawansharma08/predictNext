package com.pawan.nextpredict.data.remote.di

import com.pawan.nextpredict.data.remote.api.MlPredictionApi
import com.pawan.nextpredict.data.remote.api.YahooFinanceApi
import com.pawan.nextpredict.data.remote.api.GrokApi
import com.pawan.nextpredict.data.repository.PredictionRepositoryImpl
import com.pawan.nextpredict.domain.repository.PredictionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {

    @Binds
    @Singleton
    abstract fun bindPredictionRepository(
        impl: PredictionRepositoryImpl,
    ): PredictionRepository

    companion object {

        @Provides
        @Singleton
        fun provideYahooFinanceApi(retrofit: Retrofit): YahooFinanceApi = retrofit.create()

        @Provides
        @Singleton
        fun provideGrokApi(@Named("grok") grokRetrofit: Retrofit): GrokApi = grokRetrofit.create()

        @Provides
        @Singleton
        fun provideMlPredictionApi(@Named("ml") mlRetrofit: Retrofit): MlPredictionApi =
            mlRetrofit.create()
    }
}


