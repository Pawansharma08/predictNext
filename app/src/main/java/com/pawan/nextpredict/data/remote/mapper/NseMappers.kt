package com.pawan.nextpredict.data.remote.mapper

import com.pawan.nextpredict.data.remote.dto.*
import com.pawan.nextpredict.domain.model.*

/**
 * Model mappers to convert Alpha Vantage API DTOs into domain layer models.
 */

fun MarketStatusResponseDto.toDomain(): MarketStatus {
    // Prefer India/Bombay equity row from Alpha Vantage MARKET_STATUS response
    val indiaEntry = markets?.firstOrNull { dto ->
        dto.region?.contains("India", ignoreCase = true) == true ||
        dto.primaryExchanges?.contains("BSE", ignoreCase = true) == true ||
        dto.primaryExchanges?.contains("NSE", ignoreCase = true) == true
    }

    val isOpen: Boolean
    val statusMsg: String
    val marketLabel: String

    if (indiaEntry != null) {
        isOpen = indiaEntry.currentStatus?.equals("open", ignoreCase = true) == true
        statusMsg = if (isOpen) "BSE/NSE Markets are Open" else "BSE/NSE Markets are Closed"
        marketLabel = "India (BSE/NSE)"
    } else {
        // Fallback to first open market if India not found
        val anyOpen = markets?.firstOrNull { it.currentStatus?.equals("open", ignoreCase = true) == true }
        isOpen = anyOpen != null
        statusMsg = if (isOpen) "${anyOpen?.region ?: "Markets"} are Open" else "Markets are Closed"
        marketLabel = anyOpen?.region ?: "Global"
    }

    val mappedIndices = markets?.map { dto ->
        IndexStatus(
            indexName = "${dto.region} (${dto.marketType})",
            isOpen = dto.currentStatus?.equals("open", ignoreCase = true) == true,
            status = dto.currentStatus ?: "closed"
        )
    } ?: emptyList()

    return MarketStatus(
        isOpen = isOpen,
        statusMessage = statusMsg,
        marketType = marketLabel,
        tradeDate = indiaEntry?.localOpen ?: "",
        indices = mappedIndices
    )
}

fun GlobalQuoteResponseDto.toDomain(): StockQuote {
    val quote = globalQuote ?: GlobalQuoteDto()
    val sym = quote.symbol ?: ""
    val price = quote.price?.toDoubleOrNull() ?: 0.0
    val prevClose = quote.previousClose?.toDoubleOrNull() ?: 0.0
    val changeVal = quote.change?.toDoubleOrNull() ?: 0.0
    
    // Alpha Vantage returns percentChange as "0.7983%"
    val pctStr = quote.changePercent?.replace("%", "") ?: "0.0"
    val changePct = pctStr.toDoubleOrNull() ?: 0.0

    return StockQuote(
        symbol = sym,
        companyName = sym,
        series = "EQ",
        lastPrice = price,
        change = changeVal,
        changePercent = changePct,
        open = quote.open?.toDoubleOrNull() ?: 0.0,
        high = quote.high?.toDoubleOrNull() ?: 0.0,
        low = quote.low?.toDoubleOrNull() ?: 0.0,
        close = price,
        previousClose = prevClose,
        vwap = price,
        lowerCircuit = price * 0.9,
        upperCircuit = price * 1.1,
        yearHigh = quote.high?.toDoubleOrNull() ?: 0.0,
        yearLow = quote.low?.toDoubleOrNull() ?: 0.0,
        volume = quote.volume?.toLongOrNull() ?: 0L,
        totalTradedValue = price * (quote.volume?.toLongOrNull() ?: 0L),
        deliveryQuantity = 0L,
        deliveryPercent = 0.0,
        totalBuyQty = 0L,
        totalSellQty = 0L,
        bidAsk = emptyList(),
        isin = "",
        listingDate = null,
        faceValue = 1.0,
        issuedSize = 0L
    )
}

fun GlobalQuoteResponseDto.toStockDomain(): Stock {
    val quote = globalQuote ?: GlobalQuoteDto()
    val sym = quote.symbol ?: ""
    val price = quote.price?.toDoubleOrNull() ?: 0.0
    val changeVal = quote.change?.toDoubleOrNull() ?: 0.0
    val pctStr = quote.changePercent?.replace("%", "") ?: "0.0"
    val changePct = pctStr.toDoubleOrNull() ?: 0.0

    return Stock(
        symbol = sym,
        companyName = sym,
        lastPrice = price,
        change = changeVal,
        changePercent = changePct,
        open = quote.open?.toDoubleOrNull() ?: 0.0,
        high = quote.high?.toDoubleOrNull() ?: 0.0,
        low = quote.low?.toDoubleOrNull() ?: 0.0,
        previousClose = quote.previousClose?.toDoubleOrNull() ?: 0.0,
        volume = quote.volume?.toLongOrNull() ?: 0L,
        totalTradedValue = price * (quote.volume?.toLongOrNull() ?: 0L),
        yearHigh = quote.high?.toDoubleOrNull() ?: 0.0,
        yearLow = quote.low?.toDoubleOrNull() ?: 0.0,
        series = "EQ"
    )
}

fun SearchResponseDto.toDomain(): List<SearchResult> =
    bestMatches?.map { match ->
        SearchResult(
            symbol = match.symbol ?: "",
            companyName = match.name ?: "",
            isin = match.currency,
            series = match.type ?: "Equity"
        )
    } ?: emptyList()

fun MoverDto.toDomain(): Stock {
    val sym = ticker ?: ""
    val price = this.price?.toDoubleOrNull() ?: 0.0
    val pctStr = changePercentage?.replace("%", "") ?: "0.0"
    val changePct = pctStr.toDoubleOrNull() ?: 0.0
    val changeVal = changeAmount?.toDoubleOrNull() ?: 0.0
    val vol = volume?.toLongOrNull() ?: 0L

    return Stock(
        symbol = sym,
        companyName = sym,
        lastPrice = price,
        change = changeVal,
        changePercent = changePct,
        open = price,
        high = price,
        low = price,
        previousClose = price - changeVal,
        volume = vol,
        totalTradedValue = price * vol,
        yearHigh = price,
        yearLow = price,
        series = "EQ"
    )
}

fun TimeSeriesDailyResponseDto.toDomain(): List<HistoricalDataPoint> {
    val list = ArrayList<HistoricalDataPoint>()
    timeSeries?.forEach { (date, dp) ->
        list.add(
            HistoricalDataPoint(
                date = date,
                open = dp.open?.toDoubleOrNull() ?: 0.0,
                high = dp.high?.toDoubleOrNull() ?: 0.0,
                low = dp.low?.toDoubleOrNull() ?: 0.0,
                close = dp.close?.toDoubleOrNull() ?: 0.0,
                volume = dp.volume?.toLongOrNull() ?: 0L
            )
        )
    }
    return list.sortedBy { it.date }
}

/**
 * Maps an [IntradayResponseDto] (from TIME_SERIES_INTRADAY) into a list of
 * [HistoricalDataPoint] domain objects sorted oldest → newest.
 *
 * Alpha Vantage returns keys like "2024-01-15 09:31:00" for minute candles.
 * We pick whichever time-series map is populated (1min preferred over 5min).
 */
fun IntradayResponseDto.toDomain(): List<HistoricalDataPoint> {
    val rawMap: Map<String, IntradayDataPointDto> =
        timeSeries1min?.takeIf { it.isNotEmpty() }
            ?: timeSeries5min?.takeIf { it.isNotEmpty() }
            ?: return emptyList()

    return rawMap
        .map { (datetime, dp) ->
            HistoricalDataPoint(
                date   = datetime,                          // e.g. "2024-01-15 09:31:00"
                open   = dp.open?.toDoubleOrNull()   ?: 0.0,
                high   = dp.high?.toDoubleOrNull()   ?: 0.0,
                low    = dp.low?.toDoubleOrNull()    ?: 0.0,
                close  = dp.close?.toDoubleOrNull()  ?: 0.0,
                volume = dp.volume?.toLongOrNull()   ?: 0L,
            )
        }
        .sortedBy { it.date }  // oldest first so chart renders left-to-right
}
