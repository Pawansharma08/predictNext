package com.pawan.nextpredict.feature.market.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.domain.model.Stock
import com.pawan.nextpredict.domain.usecase.market.GetIndexConstituentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketIndexUiState(
    val indexName: String = "",
    val isLoading: Boolean = false,
    val stocks: List<Stock> = emptyList(),
    val error: AppException? = null,
)

@HiltViewModel
class MarketIndexViewModel @Inject constructor(
    private val getIndexConstituentsUseCase: GetIndexConstituentsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketIndexUiState())
    val uiState: StateFlow<MarketIndexUiState> = _uiState.asStateFlow()

    fun loadIndex(indexName: String) {
        _uiState.update { it.copy(indexName = indexName, isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getIndexConstituentsUseCase(indexName)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            stocks = result.data,
                            error = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception
                        )
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}
