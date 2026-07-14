package com.pawan.nextpredict.domain.usecase.stock

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.OptionChain
import com.pawan.nextpredict.domain.model.StockQuote
import com.pawan.nextpredict.domain.repository.StockRepository
import javax.inject.Inject

class GetStockQuoteUseCase @Inject constructor(
    private val repository: StockRepository,
) {
    suspend operator fun invoke(symbol: String): ApiResult<StockQuote> =
        repository.getStockQuote(symbol)
}

class GetOptionChainUseCase @Inject constructor(
    private val repository: StockRepository,
) {
    suspend operator fun invoke(symbol: String): ApiResult<OptionChain> =
        repository.getOptionChain(symbol)
}

class GetHistoricalDataUseCase @Inject constructor(
    private val repository: StockRepository,
) {
    suspend operator fun invoke(
        symbol: String,
        series: String = "EQ",
        fromDate: String,
        toDate: String,
    ): ApiResult<List<HistoricalDataPoint>> =
        repository.getHistoricalData(symbol, series, fromDate, toDate)
}

class GetYahooChartDataUseCase @Inject constructor(
    private val repository: StockRepository,
) {
    suspend operator fun invoke(
        symbol: String,
        interval: String,
        range: String,
    ): ApiResult<List<HistoricalDataPoint>> =
        repository.getYahooChartData(symbol, interval, range)
}

