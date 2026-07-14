package com.pawan.nextpredict.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Yahoo Finance Quote Response ──────────────────────────────────────────────

@Serializable
data class YahooQuoteResponseDto(
    @SerialName("quoteResponse")
    val quoteResponse: YahooQuoteListDto? = null
)

@Serializable
data class YahooQuoteListDto(
    @SerialName("result")
    val result: List<YahooQuoteDto>? = null,
    @SerialName("error")
    val error: String? = null
)

@Serializable
data class YahooQuoteDto(
    @SerialName("symbol")
    val symbol: String? = null,
    
    @SerialName("regularMarketPrice")
    val regularMarketPrice: Double? = null,
    
    @SerialName("regularMarketChange")
    val regularMarketChange: Double? = null,
    
    @SerialName("regularMarketChangePercent")
    val regularMarketChangePercent: Double? = null,
    
    @SerialName("regularMarketOpen")
    val regularMarketOpen: Double? = null,
    
    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: Double? = null,
    
    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: Double? = null,
    
    @SerialName("regularMarketPreviousClose")
    val regularMarketPreviousClose: Double? = null,
    
    @SerialName("regularMarketVolume")
    val regularMarketVolume: Long? = null,
    
    @SerialName("shortName")
    val shortName: String? = null,
    
    @SerialName("longName")
    val longName: String? = null,
    
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: Double? = null,
    
    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: Double? = null
)

// ─── Yahoo Finance Chart Response ──────────────────────────────────────────────

@Serializable
data class YahooChartResponseDto(
    @SerialName("chart")
    val chart: YahooChartDataDto? = null
)

@Serializable
data class YahooChartDataDto(
    @SerialName("result")
    val result: List<YahooChartResultDto>? = null,
    @SerialName("error")
    val error: YahooChartErrorDto? = null
)

@Serializable
data class YahooChartErrorDto(
    @SerialName("code")
    val code: String? = null,
    @SerialName("description")
    val description: String? = null
)

@Serializable
data class YahooChartResultDto(
    @SerialName("meta")
    val meta: YahooChartMetaDto? = null,
    @SerialName("timestamp")
    val timestamp: List<Long>? = null,
    @SerialName("indicators")
    val indicators: YahooChartIndicatorsDto? = null
)

@Serializable
data class YahooChartMetaDto(
    @SerialName("symbol")
    val symbol: String? = null,
    @SerialName("previousClose")
    val previousClose: Double? = null,
    @SerialName("chartPreviousClose")
    val chartPreviousClose: Double? = null,
    @SerialName("regularMarketPrice")
    val regularMarketPrice: Double? = null,
    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: Double? = null,
    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: Double? = null,
    @SerialName("regularMarketVolume")
    val regularMarketVolume: Long? = null,
    @SerialName("longName")
    val longName: String? = null,
    @SerialName("shortName")
    val shortName: String? = null,
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: Double? = null,
    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: Double? = null,
    @SerialName("timezone")
    val timezone: String? = null,
    @SerialName("exchangeTimezoneName")
    val exchangeTimezoneName: String? = null
)


@Serializable
data class YahooChartIndicatorsDto(
    @SerialName("quote")
    val quote: List<YahooChartQuoteDto>? = null
)

@Serializable
data class YahooChartQuoteDto(
    @SerialName("open")
    val open: List<Double?>? = null,
    @SerialName("high")
    val high: List<Double?>? = null,
    @SerialName("low")
    val low: List<Double?>? = null,
    @SerialName("close")
    val close: List<Double?>? = null,
    @SerialName("volume")
    val volume: List<Long?>? = null
)

// ─── Yahoo Finance Search Response ─────────────────────────────────────────────

@Serializable
data class YahooSearchResponseDto(
    @SerialName("quotes")
    val quotes: List<YahooSearchMatchDto>? = null
)

@Serializable
data class YahooSearchMatchDto(
    @SerialName("symbol")
    val symbol: String? = null,
    @SerialName("shortname")
    val shortName: String? = null,
    @SerialName("longname")
    val longName: String? = null,
    @SerialName("quoteType")
    val quoteType: String? = null,
    @SerialName("exchange")
    val exchange: String? = null
)
