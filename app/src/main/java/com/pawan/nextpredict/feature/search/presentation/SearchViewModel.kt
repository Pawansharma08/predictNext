package com.pawan.nextpredict.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.domain.model.SearchResult
import com.pawan.nextpredict.domain.usecase.search.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<SearchResult> = emptyList(),
    val searchHistory: List<SearchResult> = emptyList(),
    val error: AppException? = null,
) {
    val showHistory: Boolean get() = query.isEmpty() && searchHistory.isNotEmpty()
    val showResults: Boolean get() = query.isNotEmpty()
    val showEmpty: Boolean get() = query.isNotEmpty() && !isSearching && searchResults.isEmpty() && error == null
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchStocksUseCase: SearchStocksUseCase,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val addToSearchHistoryUseCase: AddToSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Observe search history from Room
        viewModelScope.launch {
            getSearchHistoryUseCase().collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, error = null) }

        // Cancel previous search
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        // Debounce: wait 300ms before searching
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearching = true) }

            when (val result = searchStocksUseCase(query)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = result.data,
                        error = null,
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isSearching = false,
                        error = result.exception,
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun onClearQuery() {
        _uiState.update { SearchUiState(searchHistory = _uiState.value.searchHistory) }
    }

    fun onResultClick(result: SearchResult) {
        viewModelScope.launch {
            addToSearchHistoryUseCase(result)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            clearSearchHistoryUseCase()
        }
    }
}
