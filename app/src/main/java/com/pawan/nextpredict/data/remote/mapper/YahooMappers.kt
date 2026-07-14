package com.pawan.nextpredict.data.remote.mapper

import com.pawan.nextpredict.data.remote.dto.*
import com.pawan.nextpredict.domain.model.*

/**
 * Model mappers to convert Yahoo Finance API DTOs into pure domain models.
 */

fun YahooChartResponseDto.toDomainQuote(): StockQuote {
    val meta = chart?.result?.firstOrNull()?.meta 
        ?: throw IllegalStateException("No metadata found in chart response")
    
    val sym = meta.symbol ?: ""
    val price = meta.regularMarketPrice ?: 0.0
    val prevClose = meta.chartPreviousClose ?: meta.previousClose ?: price
    val changeVal = price - prevClose
    val changePct = if (prevClose != 0.0) (changeVal / prevClose * 100) else 0.0

    return StockQuote(
        symbol = sym,
        companyName = meta.shortName ?: meta.longName ?: sym,
        series = "EQ",
        lastPrice = price,
        change = changeVal,
        changePercent = changePct,
        open = meta.regularMarketPrice ?: price,
        high = meta.regularMarketDayHigh ?: price,
        low = meta.regularMarketDayLow ?: price,
        close = price,
        previousClose = prevClose,
        vwap = price,
        lowerCircuit = price * 0.9,
        upperCircuit = price * 1.1,
        yearHigh = meta.fiftyTwoWeekHigh ?: price,
        yearLow = meta.fiftyTwoWeekLow ?: price,
        volume = meta.regularMarketVolume ?: 0L,
        totalTradedValue = price * (meta.regularMarketVolume ?: 0L),
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

fun YahooChartResponseDto.toDomainStock(): Stock {
    val meta = chart?.result?.firstOrNull()?.meta 
        ?: throw IllegalStateException("No metadata found in chart response")

    val sym = meta.symbol ?: ""
    val price = meta.regularMarketPrice ?: 0.0
    val prevClose = meta.chartPreviousClose ?: meta.previousClose ?: price
    val changeVal = price - prevClose
    val changePct = if (prevClose != 0.0) (changeVal / prevClose * 100) else 0.0

    return Stock(
        symbol = sym,
        companyName = meta.shortName ?: meta.longName ?: sym,
        lastPrice = price,
        change = changeVal,
        changePercent = changePct,
        open = meta.regularMarketPrice ?: price,
        high = meta.regularMarketDayHigh ?: price,
        low = meta.regularMarketDayLow ?: price,
        previousClose = prevClose,
        volume = meta.regularMarketVolume ?: 0L,
        totalTradedValue = price * (meta.regularMarketVolume ?: 0L),
        yearHigh = meta.fiftyTwoWeekHigh ?: price,
        yearLow = meta.fiftyTwoWeekLow ?: price,
        series = "EQ"
    )
}

fun YahooQuoteDto.toDomain(): StockQuote {
    val sym = symbol ?: ""
    val price = regularMarketPrice ?: 0.0
    val prevClose = regularMarketPreviousClose ?: 0.0
    val changeVal = regularMarketChange ?: 0.0
    val changePct = regularMarketChangePercent ?: 0.0

    return StockQuote(
        symbol = sym,
        companyName = shortName ?: longName ?: sym,
        series = "EQ",
        lastPrice = price,
        change = changeVal,
        changePercent = changePct,
        open = regularMarketOpen ?: price,
        high = regularMarketDayHigh ?: price,
        low = regularMarketDayLow ?: price,
        close = price,
        previousClose = prevClose,
        vwap = price,
        lowerCircuit = price * 0.9,
        upperCircuit = price * 1.1,
        yearHigh = fiftyTwoWeekHigh ?: price,
        yearLow = fiftyTwoWeekLow ?: price,
        volume = regularMarketVolume ?: 0L,
        totalTradedValue = price * (regularMarketVolume ?: 0L),
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

fun YahooQuoteDto.toStockDomain(): Stock {
    val sym = symbol ?: ""
    val price = regularMarketPrice ?: 0.0
    val changeVal = regularMarketChange ?: 0.0
    val changePct = regularMarketChangePercent ?: 0.0

    return Stock(
        symbol = sym,
        companyName = shortName ?: longName ?: sym,
        lastPrice = price,
        change = changeVal,
        changePercent = changePct,
        open = regularMarketOpen ?: price,
        high = regularMarketDayHigh ?: price,
        low = regularMarketDayLow ?: price,
        previousClose = regularMarketPreviousClose ?: price,
        volume = regularMarketVolume ?: 0L,
        totalTradedValue = price * (regularMarketVolume ?: 0L),
        yearHigh = fiftyTwoWeekHigh ?: price,
        yearLow = fiftyTwoWeekLow ?: price,
        series = "EQ"
    )
}


fun YahooSearchResponseDto.toDomain(): List<SearchResult> =
    quotes?.map { quote ->
        SearchResult(
            symbol = quote.symbol ?: "",
            companyName = quote.shortName ?: quote.longName ?: quote.symbol ?: "",
            isin = null,
            series = quote.quoteType ?: "EQUITY"
        )
    } ?: emptyList()

fun YahooChartResponseDto.toDomain(): List<HistoricalDataPoint> {
    val result = chart?.result?.firstOrNull() ?: return emptyList()
    val timestamps = result.timestamp ?: return emptyList()
    val indicators = result.indicators?.quote?.firstOrNull() ?: return emptyList()
    
    val opens = indicators.open ?: emptyList()
    val highs = indicators.high ?: emptyList()
    val lows = indicators.low ?: emptyList()
    val closes = indicators.close ?: emptyList()
    val volumes = indicators.volume ?: emptyList()

    val list = ArrayList<HistoricalDataPoint>()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    
    timestamps.forEachIndexed { i, ts ->
        // Convert timestamp (seconds) to formatted date string
        val ldt = java.time.Instant.ofEpochSecond(ts)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        val dateStr = ldt.format(formatter)

        val o = opens.getOrNull(i) ?: closes.getOrNull(i) ?: 0.0
        val h = highs.getOrNull(i) ?: maxOf(o, closes.getOrNull(i) ?: 0.0)
        val l = lows.getOrNull(i) ?: minOf(o, closes.getOrNull(i) ?: 0.0)
        val c = closes.getOrNull(i) ?: o
        val v = volumes.getOrNull(i) ?: 0L

        // Skip records with empty pricing
        if (c > 0.0) {
            list.add(
                HistoricalDataPoint(
                    date = dateStr,
                    open = o,
                    high = h,
                    low = l,
                    close = c,
                    volume = v
                )
            )
        }
    }
    return list
}
