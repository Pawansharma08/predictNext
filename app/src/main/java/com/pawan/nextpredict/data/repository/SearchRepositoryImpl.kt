package com.pawan.nextpredict.data.repository

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.safeApiCall
import com.pawan.nextpredict.core.database.dao.SearchHistoryDao
import com.pawan.nextpredict.core.database.entity.SearchHistoryEntity
import com.pawan.nextpredict.data.remote.api.YahooFinanceApi
import com.pawan.nextpredict.data.remote.mapper.toDomain
import com.pawan.nextpredict.domain.model.SearchResult
import com.pawan.nextpredict.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val api: YahooFinanceApi,
    private val dao: SearchHistoryDao,
) : SearchRepository {

    override suspend fun searchStocks(query: String): ApiResult<List<SearchResult>> = safeApiCall {
        if (query.isBlank()) return@safeApiCall emptyList()
        api.searchStocks(query).toDomain()
    }


    override fun getSearchHistory(): Flow<List<SearchResult>> =
        dao.getSearchHistory().map { list ->
            list.map { entity ->
                SearchResult(
                    symbol = entity.symbol,
                    companyName = entity.companyName,
                    isin = null,
                    series = null,
                )
            }
        }

    override suspend fun addToSearchHistory(result: SearchResult) {
        dao.insertSearchHistory(
            SearchHistoryEntity(
                symbol = result.symbol,
                companyName = result.companyName,
                searchedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun removeFromSearchHistory(symbol: String) {
        dao.deleteFromHistory(symbol)
    }

    override suspend fun clearSearchHistory() {
        dao.clearAllHistory()
    }
}
