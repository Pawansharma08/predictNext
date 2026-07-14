package com.pawan.nextpredict.feature.stockdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.domain.model.OptionChain
import com.pawan.nextpredict.domain.usecase.stock.GetOptionChainUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OptionChainUiState(
    val symbol: String = "",
    val isLoading: Boolean = false,
    val optionChain: OptionChain? = null,
    val error: AppException? = null,
    val selectedExpiry: String = "",
)

@HiltViewModel
class OptionChainViewModel @Inject constructor(
    private val getOptionChainUseCase: GetOptionChainUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptionChainUiState())
    val uiState: StateFlow<OptionChainUiState> = _uiState.asStateFlow()

    fun loadOptionChain(symbol: String) {
        _uiState.update { it.copy(symbol = symbol, isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getOptionChainUseCase(symbol)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            optionChain = result.data,
                            selectedExpiry = result.data.selectedExpiry,
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

    fun selectExpiry(expiry: String) {
        _uiState.update { it.copy(selectedExpiry = expiry) }
    }
}
