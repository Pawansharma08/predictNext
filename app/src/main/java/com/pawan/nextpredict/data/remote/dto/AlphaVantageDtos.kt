package com.pawan.nextpredict.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Global Quote ─────────────────────────────────────────────────────────────

@Serializable
data class GlobalQuoteResponseDto(
    @SerialName("Global Quote")
    val globalQuote: GlobalQuoteDto? = null,
    
    @SerialName("Information")
    val information: String? = null,
    
    @SerialName("Note")
    val note: String? = null
)


@Serializable
data class GlobalQuoteDto(
    @SerialName("01. symbol")
    val symbol: String? = null,
    
    @SerialName("02. open")
    val open: String? = null,
    
    @SerialName("03. high")
    val high: String? = null,
    
    @SerialName("04. low")
    val low: String? = null,
    
    @SerialName("05. price")
    val price: String? = null,
    
    @SerialName("06. volume")
    val volume: String? = null,
    
    @SerialName("07. latest trading day")
    val latestTradingDay: String? = null,
    
    @SerialName("08. previous close")
    val previousClose: String? = null,
    
    @SerialName("09. change")
    val change: String? = null,
    
    @SerialName("10. change percent")
    val changePercent: String? = null
)

// ─── Search / Autocomplete ───────────────────────────────────────────────────

@Serializable
data class SearchResponseDto(
    @SerialName("bestMatches")
    val bestMatches: List<SearchMatchDto>? = null
)

@Serializable
data class SearchMatchDto(
    @SerialName("1. symbol")
    val symbol: String? = null,
    
    @SerialName("2. name")
    val name: String? = null,
    
    @SerialName("3. type")
    val type: String? = null,
    
    @SerialName("4. region")
    val region: String? = null,
    
    @SerialName("5. marketOpen")
    val marketOpen: String? = null,
    
    @SerialName("6. marketClose")
    val marketClose: String? = null,
    
    @SerialName("7. timezone")
    val timezone: String? = null,
    
    @SerialName("8. currency")
    val currency: String? = null,
    
    @SerialName("9. matchScore")
    val matchScore: String? = null
)

// ─── Market Status ────────────────────────────────────────────────────────────

@Serializable
data class MarketStatusResponseDto(
    @SerialName("endpoint")
    val endpoint: String? = null,
    
    @SerialName("markets")
    val markets: List<MarketStatusDto>? = null
)

@Serializable
data class MarketStatusDto(
    @SerialName("market_type")
    val marketType: String? = null,
    
    @SerialName("region")
    val region: String? = null,
    
    @SerialName("primary_exchanges")
    val primaryExchanges: String? = null,
    
    @SerialName("local_open")
    val localOpen: String? = null,
    
    @SerialName("local_close")
    val localClose: String? = null,
    
    @SerialName("current_status")
    val currentStatus: String? = null,
    
    @SerialName("notes")
    val notes: String? = null
)

// ─── Top Gainers, Losers, and Most Actives ─────────────────────────────────────

@Serializable
data class TopMoversResponseDto(
    @SerialName("top_gainers")
    val topGainers: List<MoverDto>? = null,
    
    @SerialName("top_losers")
    val topLosers: List<MoverDto>? = null,
    
    @SerialName("most_actively_traded")
    val mostActive: List<MoverDto>? = null
)

@Serializable
data class MoverDto(
    @SerialName("ticker")
    val ticker: String? = null,
    
    @SerialName("price")
    val price: String? = null,
    
    @SerialName("change_amount")
    val changeAmount: String? = null,
    
    @SerialName("change_percentage")
    val changePercentage: String? = null,
    
    @SerialName("volume")
    val volume: String? = null
)

// ─── Time Series Daily ────────────────────────────────────────────────────────

@Serializable
data class TimeSeriesDailyResponseDto(
    @SerialName("Meta Data")
    val metaData: DailyMetaDto? = null,
    
    @SerialName("Time Series (Daily)")
    val timeSeries: Map<String, DailyDataPointDto>? = null,
    
    @SerialName("Information")
    val information: String? = null,
    
    @SerialName("Note")
    val note: String? = null
)


@Serializable
data class DailyMetaDto(
    @SerialName("1. Information")
    val information: String? = null,
    
    @SerialName("2. Symbol")
    val symbol: String? = null,
    
    @SerialName("3. Last Refreshed")
    val lastRefreshed: String? = null,
    
    @SerialName("4. Output Size")
    val outputSize: String? = null,
    
    @SerialName("5. Time Zone")
    val timeZone: String? = null
)

@Serializable
data class DailyDataPointDto(
    @SerialName("1. open")
    val open: String? = null,
    
    @SerialName("2. high")
    val high: String? = null,
    
    @SerialName("3. low")
    val low: String? = null,
    
    @SerialName("4. close")
    val close: String? = null,
    
    @SerialName("5. volume")
    val volume: String? = null
)

// ─── Time Series Intraday (1min, 5min, etc.) ──────────────────────────────────

@Serializable
data class IntradayResponseDto(
    @SerialName("Meta Data")
    val metaData: IntradayMetaDto? = null,

    // The key changes with the interval: "Time Series (1min)", "Time Series (5min)", etc.
    // We capture all possible interval keys by using a custom serializer workaround:
    // The actual map is deserialized via a dedicated wrapper in the mapper.
    @SerialName("Time Series (1min)")
    val timeSeries1min: Map<String, IntradayDataPointDto>? = null,

    @SerialName("Time Series (5min)")
    val timeSeries5min: Map<String, IntradayDataPointDto>? = null,
    
    @SerialName("Information")
    val information: String? = null,
    
    @SerialName("Note")
    val note: String? = null
)


@Serializable
data class IntradayMetaDto(
    @SerialName("1. Information")
    val information: String? = null,

    @SerialName("2. Symbol")
    val symbol: String? = null,

    @SerialName("3. Last Refreshed")
    val lastRefreshed: String? = null,

    @SerialName("4. Interval")
    val interval: String? = null,

    @SerialName("5. Output Size")
    val outputSize: String? = null,

    @SerialName("6. Time Zone")
    val timeZone: String? = null
)

@Serializable
data class IntradayDataPointDto(
    @SerialName("1. open")
    val open: String? = null,

    @SerialName("2. high")
    val high: String? = null,

    @SerialName("3. low")
    val low: String? = null,

    @SerialName("4. close")
    val close: String? = null,

    @SerialName("5. volume")
    val volume: String? = null
)
