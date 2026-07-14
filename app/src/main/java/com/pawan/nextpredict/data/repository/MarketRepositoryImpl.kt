package com.pawan.nextpredict.data.repository

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.safeApiCall
import com.pawan.nextpredict.data.remote.api.AlphaVantageApi
import com.pawan.nextpredict.data.remote.mapper.*
import com.pawan.nextpredict.domain.model.*
import com.pawan.nextpredict.domain.repository.MarketRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val api: AlphaVantageApi,
) : MarketRepository {

    override suspend fun getMarketStatus(): ApiResult<MarketStatus> = safeApiCall {
        api.getMarketStatus().toDomain()
    }

    override suspend fun getAllIndices(): ApiResult<List<Index>> = safeApiCall {
        // Query Indian BSE blue-chip stocks as live index proxies
        // RELIANCE.BSE  → Nifty 50 bellwether
        // HDFCBANK.BSE  → Banking sector leader
        // INFY.BSE      → Nifty IT bellwether
        val reliance  = api.getGlobalQuote("RELIANCE.BSE").toDomain()
        val hdfc      = api.getGlobalQuote("HDFCBANK.BSE").toDomain()
        val infy      = api.getGlobalQuote("INFY.BSE").toDomain()

        listOf(
            Index(
                name = "Reliance (BSE)",
                lastPrice = reliance.lastPrice,
                change = reliance.change,
                changePercent = reliance.changePercent,
                open = reliance.open,
                high = reliance.high,
                low = reliance.low,
                previousClose = reliance.previousClose,
                advances = 0,
                declines = 0,
                unchanged = 0
            ),
            Index(
                name = "HDFC Bank (BSE)",
                lastPrice = hdfc.lastPrice,
                change = hdfc.change,
                changePercent = hdfc.changePercent,
                open = hdfc.open,
                high = hdfc.high,
                low = hdfc.low,
                previousClose = hdfc.previousClose,
                advances = 0,
                declines = 0,
                unchanged = 0
            ),
            Index(
                name = "Infosys (BSE)",
                lastPrice = infy.lastPrice,
                change = infy.change,
                changePercent = infy.changePercent,
                open = infy.open,
                high = infy.high,
                low = infy.low,
                previousClose = infy.previousClose,
                advances = 0,
                declines = 0,
                unchanged = 0
            )
        )
    }

    override suspend fun getIndexConstituents(indexName: String): ApiResult<List<Stock>> =
        safeApiCall {
            val movers = api.getTopGainersLosers()
            val list = when {
                indexName.contains("Infy", ignoreCase = true) || indexName.contains("IT", ignoreCase = true) -> {
                    movers.topGainers?.map { it.toDomain() }
                }
                indexName.contains("Losers", ignoreCase = true) -> {
                    movers.topLosers?.map { it.toDomain() }
                }
                else -> {
                    movers.mostActive?.map { it.toDomain() }
                }
            }
            list ?: emptyList()
        }

    override suspend fun getTopGainers(indexName: String): ApiResult<List<Stock>> = safeApiCall {
        api.getTopGainersLosers().topGainers?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getTopLosers(indexName: String): ApiResult<List<Stock>> = safeApiCall {
        api.getTopGainersLosers().topLosers?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getMostActive(): ApiResult<List<Stock>> = safeApiCall {
        api.getTopGainersLosers().mostActive?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getPreOpenMarket(key: String): ApiResult<List<PreOpenStock>> =
        safeApiCall {
            // Map top gainers to simulate pre-open market
            api.getTopGainersLosers().mostActive?.map { mover ->
                val stock = mover.toDomain()
                PreOpenStock(
                    symbol = stock.symbol,
                    iep = stock.lastPrice,
                    change = stock.change,
                    changePercent = stock.changePercent,
                    finalQuantity = stock.volume,
                    totalBuyQty = 0L,
                    totalSellQty = 0L
                )
            } ?: emptyList()
        }
}
