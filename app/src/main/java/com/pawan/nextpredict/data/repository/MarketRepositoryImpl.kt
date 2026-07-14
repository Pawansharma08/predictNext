package com.pawan.nextpredict.data.repository

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.safeApiCall
import com.pawan.nextpredict.data.remote.api.YahooFinanceApi
import com.pawan.nextpredict.data.remote.mapper.*
import com.pawan.nextpredict.domain.model.*
import com.pawan.nextpredict.domain.repository.MarketRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val api: YahooFinanceApi,
) : MarketRepository {

    companion object {
        private const val SENSEX_TICKER = "^BSESN"
        private const val NIFTY_TICKER  = "^NSEI"
        
        // NSE Movers list
        private val MOVERS_SYMBOL_LIST = listOf(
            "RELIANCE.NS", "TCS.NS", "HDFCBANK.NS", "INFY.NS", "ICICIBANK.NS", "SBIN.NS"
        )
    }

    override suspend fun getMarketStatus(): ApiResult<MarketStatus> = safeApiCall {
        val response = api.getChartData(NIFTY_TICKER, "1d", "1d")
        val isMarketOpen = response.chart?.result?.firstOrNull() != null
        val statusMsg = if (isMarketOpen) "NSE/BSE Markets Active" else "NSE/BSE Markets Closed"

        MarketStatus(
            isOpen = isMarketOpen,
            statusMessage = statusMsg,
            marketType = "NSE/BSE",
            tradeDate = "",
            indices = listOf(
                IndexStatus("Nifty 50", isMarketOpen, if (isMarketOpen) "active" else "closed"),
                IndexStatus("Sensex", isMarketOpen, if (isMarketOpen) "active" else "closed")
            )
        )
    }

    override suspend fun getAllIndices(): ApiResult<List<Index>> = safeApiCall {
        coroutineScope {
            val niftyDeferred = async { api.getChartData(NIFTY_TICKER, "1d", "1d").toDomainQuote() }
            val sensexDeferred = async { api.getChartData(SENSEX_TICKER, "1d", "1d").toDomainQuote() }

            val nifty = niftyDeferred.await()
            val sensex = sensexDeferred.await()

            listOf(
                Index(
                    name = "Nifty 50",
                    lastPrice = nifty.lastPrice,
                    change = nifty.change,
                    changePercent = nifty.changePercent,
                    open = nifty.open,
                    high = nifty.high,
                    low = nifty.low,
                    previousClose = nifty.previousClose,
                    advances = 0,
                    declines = 0,
                    unchanged = 0
                ),
                Index(
                    name = "Sensex",
                    lastPrice = sensex.lastPrice,
                    change = sensex.change,
                    changePercent = sensex.changePercent,
                    open = sensex.open,
                    high = sensex.high,
                    low = sensex.low,
                    previousClose = sensex.previousClose,
                    advances = 0,
                    declines = 0,
                    unchanged = 0
                )
            )
        }
    }

    private suspend fun fetchMoversList(): List<Stock> = coroutineScope {
        MOVERS_SYMBOL_LIST.map { sym ->
            async {
                runCatching {
                    api.getChartData(sym, "1d", "1d").toDomainStock()
                }.getOrNull()
            }
        }.mapNotNull { it.await() }
    }

    override suspend fun getIndexConstituents(indexName: String): ApiResult<List<Stock>> =
        safeApiCall {
            val stocks = fetchMoversList()
            stocks.sortedByDescending { it.changePercent }
        }

    override suspend fun getTopGainers(indexName: String): ApiResult<List<Stock>> = safeApiCall {
        val stocks = fetchMoversList()
        stocks.sortedByDescending { it.changePercent }
    }

    override suspend fun getTopLosers(indexName: String): ApiResult<List<Stock>> = safeApiCall {
        val stocks = fetchMoversList()
        stocks.sortedBy { it.changePercent }
    }

    override suspend fun getMostActive(): ApiResult<List<Stock>> = safeApiCall {
        val stocks = fetchMoversList()
        stocks.sortedByDescending { it.volume }
    }

    override suspend fun getPreOpenMarket(key: String): ApiResult<List<PreOpenStock>> =
        safeApiCall {
            val stocks = fetchMoversList()
            stocks.map { stock ->
                PreOpenStock(
                    symbol = stock.symbol,
                    iep = stock.lastPrice,
                    change = stock.change,
                    changePercent = stock.changePercent,
                    finalQuantity = stock.volume,
                    totalBuyQty = 0L,
                    totalSellQty = 0L
                )
            }
        }
}


