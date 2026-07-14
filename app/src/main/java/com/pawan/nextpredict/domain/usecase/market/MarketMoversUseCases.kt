package com.pawan.nextpredict.domain.usecase.market

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.domain.model.Stock
import com.pawan.nextpredict.domain.repository.MarketRepository
import javax.inject.Inject

class GetTopGainersUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(indexName: String = "NIFTY 50"): ApiResult<List<Stock>> =
        repository.getTopGainers(indexName)
}

class GetTopLosersUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(indexName: String = "NIFTY 50"): ApiResult<List<Stock>> =
        repository.getTopLosers(indexName)
}

class GetMostActiveUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(): ApiResult<List<Stock>> =
        repository.getMostActive()
}

class GetIndexConstituentsUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(indexName: String): ApiResult<List<Stock>> =
        repository.getIndexConstituents(indexName)
}
