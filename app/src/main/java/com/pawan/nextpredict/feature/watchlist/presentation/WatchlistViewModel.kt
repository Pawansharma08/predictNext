package com.pawan.nextpredict.feature.watchlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.domain.model.Watchlist
import com.pawan.nextpredict.domain.model.WatchlistStock
import com.pawan.nextpredict.domain.usecase.watchlist.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val isLoading: Boolean = true,
    val watchlists: List<Watchlist> = emptyList(),
    val selectedWatchlistId: Long = -1L,
    val items: List<WatchlistStock> = emptyList(),
    val error: AppException? = null,
    val showCreateDialog: Boolean = false,
    val newWatchlistName: String = "",
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val getAllWatchlistsUseCase: GetAllWatchlistsUseCase,
    private val getWatchlistItemsUseCase: GetWatchlistItemsUseCase,
    private val createWatchlistUseCase: CreateWatchlistUseCase,
    private val deleteWatchlistUseCase: DeleteWatchlistUseCase,
    private val renameWatchlistUseCase: RenameWatchlistUseCase,
    private val removeStockFromWatchlistUseCase: RemoveStockFromWatchlistUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    private var itemsJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            getAllWatchlistsUseCase().collect { watchlists ->
                val firstId = watchlists.firstOrNull()?.id ?: -1L
                val selectedId = if (_uiState.value.selectedWatchlistId == -1L)
                    firstId else _uiState.value.selectedWatchlistId

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        watchlists = watchlists,
                        selectedWatchlistId = selectedId,
                    )
                }

                if (selectedId != -1L) {
                    observeItems(selectedId)
                }
            }
        }
    }

    fun selectWatchlist(id: Long) {
        _uiState.update { it.copy(selectedWatchlistId = id, items = emptyList()) }
        observeItems(id)
    }

    private fun observeItems(watchlistId: Long) {
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            getWatchlistItemsUseCase(watchlistId).collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, newWatchlistName = "") }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, newWatchlistName = "") }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(newWatchlistName = name) }
    }

    fun createWatchlist() {
        val name = _uiState.value.newWatchlistName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val newId = createWatchlistUseCase(name)
            _uiState.update { it.copy(showCreateDialog = false, newWatchlistName = "") }
            selectWatchlist(newId)
        }
    }

    fun deleteWatchlist(watchlistId: Long) {
        viewModelScope.launch {
            deleteWatchlistUseCase(watchlistId)
        }
    }

    fun removeStock(symbol: String) {
        val watchlistId = _uiState.value.selectedWatchlistId
        if (watchlistId == -1L) return
        viewModelScope.launch {
            removeStockFromWatchlistUseCase(watchlistId, symbol)
        }
    }
}
