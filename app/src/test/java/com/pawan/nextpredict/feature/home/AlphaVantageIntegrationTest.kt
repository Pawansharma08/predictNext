package com.pawan.nextpredict.feature.home

import com.pawan.nextpredict.data.remote.api.AlphaVantageApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import com.pawan.nextpredict.core.network.AlphaVantageApiKeyInterceptor
import kotlinx.coroutines.runBlocking

class AlphaVantageIntegrationTest {

    @org.junit.Ignore("Disabled to prevent hitting hourly/daily rate limits on Alpha Vantage in CI")
    @Test
    fun testAlphaVantageRealConnection() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val apiKeyInterceptor = AlphaVantageApiKeyInterceptor()

        val client = OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.alphavantage.co/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(AlphaVantageApi::class.java)

        println("=== Starting Alpha Vantage Integration Test ===")
        
        try {
            runBlocking {
                println("1. Fetching Global Market Status...")
                val status = api.getMarketStatus()
                println("Market Status: $status")

                println("2. Fetching Top Movers...")
                val movers = api.getTopGainersLosers()
                println("Movers: ${movers.topGainers?.size} gainers, ${movers.topLosers?.size} losers")

                println("3. Fetching IBM Quote...")
                val quote = api.getGlobalQuote("IBM")
                println("IBM Quote: $quote")

                assert(status.markets != null)
                assert(movers.mostActive != null)
                assert(quote.globalQuote != null)
            }
            println("=== Test PASSED ===")
        } catch (e: Exception) {
            println("=== Test FAILED ===")
            e.printStackTrace()
            throw e
        }
    }
}
