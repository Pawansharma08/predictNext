package com.pawan.nextpredict.feature.market.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.core.network.NseApiConstants
import com.pawan.nextpredict.domain.model.Stock
import com.pawan.nextpredict.domain.usecase.market.GetIndexConstituentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val isLoading: Boolean = false,
    val stocks: List<Stock> = emptyList(),
    val selectedIndex: String = NseApiConstants.Index.NIFTY_50,
    val error: AppException? = null,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val getIndexConstituentsUseCase: GetIndexConstituentsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState(isLoading = true))
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    val availableIndices = listOf(
        NseApiConstants.Index.NIFTY_50,
        NseApiConstants.Index.NIFTY_BANK,
        NseApiConstants.Index.NIFTY_IT,
        NseApiConstants.Index.NIFTY_PHARMA,
        NseApiConstants.Index.NIFTY_FMCG,
        NseApiConstants.Index.NIFTY_AUTO,
        NseApiConstants.Index.NIFTY_METAL,
    )

    init {
        loadIndexData(NseApiConstants.Index.NIFTY_50)
    }

    fun selectIndex(indexName: String) {
        _uiState.update { it.copy(selectedIndex = indexName, isLoading = true, error = null) }
        loadIndexData(indexName)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadIndexData(_uiState.value.selectedIndex, isRefresh = true)
    }

    private fun loadIndexData(indexName: String, isRefresh: Boolean = false) {
        viewModelScope.launch {
            when (val result = getIndexConstituentsUseCase(indexName)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        stocks = result.data,
                        error = null,
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.exception,
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}
