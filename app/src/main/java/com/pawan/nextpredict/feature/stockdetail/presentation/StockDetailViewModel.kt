package com.pawan.nextpredict.feature.stockdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.PricePrediction
import com.pawan.nextpredict.domain.model.StockQuote
import com.pawan.nextpredict.domain.usecase.stock.GetHistoricalDataUseCase
import com.pawan.nextpredict.domain.usecase.stock.GetPricePredictionUseCase
import com.pawan.nextpredict.domain.usecase.watchlist.IsSymbolInWatchlistUseCase
import com.pawan.nextpredict.domain.usecase.watchlist.AddStockToWatchlistUseCase
import com.pawan.nextpredict.domain.usecase.watchlist.RemoveStockFromWatchlistUseCase
import com.pawan.nextpredict.domain.usecase.stock.GetStockQuoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class StockDetailUiState(
    val symbol: String = "",
    val isLoading: Boolean = false,
    val quote: StockQuote? = null,
    val error: AppException? = null,
    val isInWatchlist: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedChartPeriod: ChartPeriod = ChartPeriod.ONE_DAY,
    val chartPoints: List<HistoricalDataPoint> = emptyList(),
    val isChartLoading: Boolean = false,
    // Live data indicator
    val isLive: Boolean = false,
    val lastUpdatedAt: String = "",
    // Grok prediction state
    val prediction: PricePrediction? = null,
    val isPredicting: Boolean = false,
    val predictionError: String? = null,
)

enum class ChartPeriod(val label: String, val days: Int) {
    ONE_DAY("1D", 1),
    ONE_WEEK("1W", 7),
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365),
    FIVE_YEARS("5Y", 1825),
}

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val getStockQuoteUseCase: GetStockQuoteUseCase,
    private val getHistoricalDataUseCase: GetHistoricalDataUseCase,
    private val isSymbolInWatchlistUseCase: IsSymbolInWatchlistUseCase,
    private val addStockToWatchlistUseCase: AddStockToWatchlistUseCase,
    private val removeStockFromWatchlistUseCase: RemoveStockFromWatchlistUseCase,
    private val getPricePredictionUseCase: GetPricePredictionUseCase,
) : ViewModel() {

    companion object {
        /**
         * Auto-refresh interval in milliseconds.
         * 5 minutes = safe for Alpha Vantage free tier (25 calls/day).
         * Change to 60_000L if you have a premium API key.
         */
        private const val AUTO_REFRESH_INTERVAL_MS = 5 * 60 * 1000L
    }

    /** Holds the coroutine that auto-polls for fresh price data. */
    private var pollingJob: Job? = null

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _uiState = MutableStateFlow(StockDetailUiState(isLoading = true))
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    fun loadStock(symbol: String) {
        _uiState.update { it.copy(symbol = symbol, isLoading = true, error = null) }

        // Observe watchlist status
        viewModelScope.launch {
            isSymbolInWatchlistUseCase(watchlistId = 1L, symbol = symbol).collect { isIn ->
                _uiState.update { it.copy(isInWatchlist = isIn) }
            }
        }

        // Initial data load
        viewModelScope.launch {
            fetchQuoteAndChart(symbol, isInitialLoad = true)
        }

        // Start auto-polling every AUTO_REFRESH_INTERVAL_MS
        startPolling(symbol)
    }

    /**
     * Starts a background loop that refreshes the quote automatically.
     * Previous polling job is cancelled before starting a new one.
     */
    private fun startPolling(symbol: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                fetchQuoteAndChart(symbol, isInitialLoad = false)
            }
        }
    }

    private suspend fun fetchQuoteAndChart(symbol: String, isInitialLoad: Boolean) {
        if (!isInitialLoad) {
            _uiState.update { it.copy(isRefreshing = true) }
        }
        when (val result = getStockQuoteUseCase(symbol)) {
            is ApiResult.Success -> {
                val timestamp = LocalTime.now().format(timeFormatter)
                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        isRefreshing  = false,
                        quote         = result.data,
                        error         = null,
                        isLive        = true,
                        lastUpdatedAt = timestamp,
                    )
                }
                loadChartData(symbol, _uiState.value.selectedChartPeriod)
            }
            is ApiResult.Error -> _uiState.update {
                it.copy(
                    isLoading    = false,
                    isRefreshing = false,
                    error        = result.exception,
                    isLive       = false,
                )
            }
            ApiResult.Loading -> Unit
        }
    }

    /** Caches daily history points to avoid redundant network calls during prediction. */
    private var cachedDailyHistory: List<HistoricalDataPoint> = emptyList()

    private fun loadChartData(symbol: String, period: ChartPeriod) {
        _uiState.update { it.copy(isChartLoading = true) }
        viewModelScope.launch {
            val result = getHistoricalDataUseCase(
                symbol = symbol,
                fromDate = "",
                toDate = ""
            )
            when (result) {
                is ApiResult.Success -> {
                    cachedDailyHistory = result.data
                    val limit = when (period) {
                        ChartPeriod.ONE_DAY -> 7
                        ChartPeriod.ONE_WEEK -> 15
                        ChartPeriod.ONE_MONTH -> 30
                        ChartPeriod.THREE_MONTHS -> 60
                        else -> 100
                    }
                    val points = result.data.takeLast(limit).toList()
                    _uiState.update { it.copy(isChartLoading = false, chartPoints = points) }
                }
                else -> {
                    _uiState.update { it.copy(isChartLoading = false, chartPoints = emptyList()) }
                }
            }
        }
    }

    fun refresh() {
        val symbol = _uiState.value.symbol
        viewModelScope.launch {
            fetchQuoteAndChart(symbol, isInitialLoad = false)
        }
    }

    fun selectChartPeriod(period: ChartPeriod) {
        _uiState.update { it.copy(selectedChartPeriod = period) }
        loadChartData(_uiState.value.symbol, period)
    }

    fun toggleWatchlist() {
        val state = _uiState.value
        val quote = state.quote ?: return

        viewModelScope.launch {
            if (state.isInWatchlist) {
                removeStockFromWatchlistUseCase(watchlistId = 1L, symbol = quote.symbol)
            } else {
                addStockToWatchlistUseCase(
                    watchlistId = 1L,
                    symbol = quote.symbol,
                    companyName = quote.companyName,
                )
            }
        }
    }

    fun requestPrediction() {
        val state = _uiState.value
        val currentPrice = state.quote?.lastPrice ?: return
        val symbol = state.symbol.ifBlank { return }

        _uiState.update { it.copy(isPredicting = true, predictionError = null) }

        viewModelScope.launch {
            when (val result = getPricePredictionUseCase(symbol, currentPrice, cachedDailyHistory)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isPredicting = false, prediction = result.data, predictionError = null)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isPredicting = false,
                        predictionError = result.exception.message ?: "Prediction failed",
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
