package com.pawan.nextpredict.domain.usecase.market

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.domain.model.MarketStatus
import com.pawan.nextpredict.domain.repository.MarketRepository
import javax.inject.Inject

class GetMarketStatusUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(): ApiResult<MarketStatus> =
        repository.getMarketStatus()
}
