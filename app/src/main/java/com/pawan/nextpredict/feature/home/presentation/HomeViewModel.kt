package com.pawan.nextpredict.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.domain.model.Index
import com.pawan.nextpredict.domain.model.MarketStatus
import com.pawan.nextpredict.domain.model.Stock
import com.pawan.nextpredict.domain.usecase.market.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val marketStatus: MarketStatus? = null,
    val indices: List<Index> = emptyList(),
    val topGainers: List<Stock> = emptyList(),
    val topLosers: List<Stock> = emptyList(),
    val mostActive: List<Stock> = emptyList(),
    val error: AppException? = null,
    val isRefreshing: Boolean = false,
) {
    val hasData: Boolean
        get() = indices.isNotEmpty() || topGainers.isNotEmpty()
}

sealed class HomeEvent {
    data object Refresh : HomeEvent()
    data class StockClicked(val symbol: String) : HomeEvent()
    data object ViewAllGainersClicked : HomeEvent()
    data object ViewAllLosersClicked : HomeEvent()
    data object ViewAllActiveClicked : HomeEvent()
}

sealed class HomeEffect {
    data class NavigateToStock(val symbol: String) : HomeEffect()
    data object NavigateToGainers : HomeEffect()
    data object NavigateToLosers : HomeEffect()
    data object NavigateToActive : HomeEffect()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMarketStatusUseCase: GetMarketStatusUseCase,
    private val getIndicesUseCase: GetIndicesUseCase,
    private val getTopGainersUseCase: GetTopGainersUseCase,
    private val getTopLosersUseCase: GetTopLosersUseCase,
    private val getMostActiveUseCase: GetMostActiveUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<HomeEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<HomeEffect> = _effects.asSharedFlow()

    init {
        loadHomeData()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Refresh -> refresh()
            is HomeEvent.StockClicked -> {
                _effects.tryEmit(HomeEffect.NavigateToStock(event.symbol))
            }
            HomeEvent.ViewAllGainersClicked -> _effects.tryEmit(HomeEffect.NavigateToGainers)
            HomeEvent.ViewAllLosersClicked -> _effects.tryEmit(HomeEffect.NavigateToLosers)
            HomeEvent.ViewAllActiveClicked -> _effects.tryEmit(HomeEffect.NavigateToActive)
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        loadHomeData(isRefresh = true)
    }

    private fun loadHomeData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            // Load all data concurrently
            val marketStatusDeferred = async { getMarketStatusUseCase() }
            val indicesDeferred = async { getIndicesUseCase() }
            val gainersDeferred = async { getTopGainersUseCase() }
            val losersDeferred = async { getTopLosersUseCase() }
            val mostActiveDeferred = async { getMostActiveUseCase() }

            val marketStatusResult = marketStatusDeferred.await()
            val indicesResult = indicesDeferred.await()
            val gainersResult = gainersDeferred.await()
            val losersResult = losersDeferred.await()
            val mostActiveResult = mostActiveDeferred.await()

            // Check for critical error (indices failing = nothing to show)
            if (indicesResult is ApiResult.Error && !_uiState.value.hasData) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = indicesResult.exception,
                    )
                }
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    marketStatus = (marketStatusResult as? ApiResult.Success)?.data,
                    indices = (indicesResult as? ApiResult.Success)?.data?.take(5) ?: state.indices,
                    topGainers = (gainersResult as? ApiResult.Success)?.data?.take(10) ?: state.topGainers,
                    topLosers = (losersResult as? ApiResult.Success)?.data?.take(10) ?: state.topLosers,
                    mostActive = (mostActiveResult as? ApiResult.Success)?.data?.take(10) ?: state.mostActive,
                )
            }
        }
    }
}
