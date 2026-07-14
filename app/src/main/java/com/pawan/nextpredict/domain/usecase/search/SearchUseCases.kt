package com.pawan.nextpredict.domain.usecase.search

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.domain.model.SearchResult
import com.pawan.nextpredict.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchStocksUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(query: String): ApiResult<List<SearchResult>> =
        repository.searchStocks(query)
}

class GetSearchHistoryUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    operator fun invoke(): Flow<List<SearchResult>> =
        repository.getSearchHistory()
}

class AddToSearchHistoryUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(result: SearchResult) =
        repository.addToSearchHistory(result)
}

class ClearSearchHistoryUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke() = repository.clearSearchHistory()
}
