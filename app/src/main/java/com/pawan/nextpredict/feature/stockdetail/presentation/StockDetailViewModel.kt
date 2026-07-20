package com.pawan.nextpredict.feature.stockdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.PredictionDirection
import com.pawan.nextpredict.domain.model.PredictionResult
import com.pawan.nextpredict.domain.model.PricePrediction
import com.pawan.nextpredict.domain.model.StockQuote
import kotlin.math.abs
import com.pawan.nextpredict.domain.usecase.stock.GetStockQuoteUseCase
import com.pawan.nextpredict.domain.usecase.stock.GetHistoricalDataUseCase
import com.pawan.nextpredict.domain.usecase.stock.GetYahooChartDataUseCase
import com.pawan.nextpredict.domain.usecase.stock.GetPricePredictionUseCase
import com.pawan.nextpredict.domain.usecase.watchlist.IsSymbolInWatchlistUseCase
import com.pawan.nextpredict.domain.usecase.watchlist.AddStockToWatchlistUseCase
import com.pawan.nextpredict.domain.usecase.watchlist.RemoveStockFromWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    val selectedChartPeriod: ChartPeriod = ChartPeriod.FIVE_MINUTES,
    val chartPoints: List<HistoricalDataPoint> = emptyList(),
    val isChartLoading: Boolean = false,
    // Live data indicator
    val isLive: Boolean = false,
    val lastUpdatedAt: String = "",
    // Technical analysis prediction state
    val prediction: PricePrediction? = null,
    val isPredicting: Boolean = false,
    val predictionError: String? = null,
    // Prediction verification (after 5 min)
    val predictionCountdownSeconds: Int = 0,
    val isCountdownActive: Boolean = false,
    val predictionResult: PredictionResult? = null,
    val priceAtPrediction: Double = 0.0,
)

enum class ChartPeriod(val label: String, val days: Int) {
    FIVE_MINUTES("5m", 1),
    ONE_HOUR("1hr", 7),
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
    private val getYahooChartDataUseCase: GetYahooChartDataUseCase,
    private val isSymbolInWatchlistUseCase: IsSymbolInWatchlistUseCase,
    private val addStockToWatchlistUseCase: AddStockToWatchlistUseCase,
    private val removeStockFromWatchlistUseCase: RemoveStockFromWatchlistUseCase,
    private val getPricePredictionUseCase: GetPricePredictionUseCase,
) : ViewModel() {

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MS = 5 * 1000L
        private const val VERIFICATION_SECONDS = 5 * 60
    }

    /** Holds the coroutine that auto-polls for fresh price data. */
    private var pollingJob: Job? = null

    /** Holds the countdown + verification coroutine after a prediction is made. */
    private var verificationJob: Job? = null

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

        // Pre-fetch 1-year daily history in the background to cache for technical predictions
        viewModelScope.launch {
            val dailyResult = getYahooChartDataUseCase(symbol, interval = "1d", range = "1y")
            if (dailyResult is ApiResult.Success) {
                cachedDailyHistory = dailyResult.data
            }
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
            var secondsPassed = 0
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                secondsPassed += (AUTO_REFRESH_INTERVAL_MS / 1000).toInt()

                val currentPeriod = _uiState.value.selectedChartPeriod
                val shouldRefreshChart = when (currentPeriod) {
                    ChartPeriod.FIVE_MINUTES -> secondsPassed >= 30
                    ChartPeriod.ONE_HOUR -> secondsPassed >= 60
                    else -> false
                }

                if (shouldRefreshChart) {
                    secondsPassed = 0
                    loadChartData(symbol, currentPeriod, isSilent = true)
                }

                fetchQuoteAndChart(symbol, isInitialLoad = false)
            }
        }
    }

    private suspend fun fetchQuoteAndChart(symbol: String, isInitialLoad: Boolean) {
        if (!isInitialLoad) {
            // Quiet refresh for polling so it doesn't show loading indicator
            _uiState.update { it.copy(isRefreshing = false) }
        }
        when (val result = getStockQuoteUseCase(symbol)) {
            is ApiResult.Success -> {
                val currentPrice = result.data.lastPrice
                val timestamp = LocalTime.now().format(timeFormatter)
                
                _uiState.update { state ->
                    // Stretch the last candle locally to create the "live moving candle" effect
                    val updatedPoints = state.chartPoints.toMutableList()
                    if (updatedPoints.isNotEmpty()) {
                        val lastIdx = updatedPoints.lastIndex
                        val lastPoint = updatedPoints[lastIdx]
                        updatedPoints[lastIdx] = lastPoint.copy(
                            close = currentPrice,
                            high = maxOf(lastPoint.high, currentPrice),
                            low = if (lastPoint.low == 0.0) currentPrice else minOf(lastPoint.low, currentPrice)
                        )
                    }
                    state.copy(
                        isLoading     = false,
                        isRefreshing  = false,
                        quote         = result.data,
                        error         = null,
                        isLive        = true,
                        lastUpdatedAt = timestamp,
                        chartPoints   = updatedPoints
                    )
                }
                
                // Only load historical chart data from network on initial load
                if (isInitialLoad) {
                    loadChartData(symbol, _uiState.value.selectedChartPeriod)
                }
            }
            is ApiResult.Error -> {
                if (isInitialLoad) {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            isRefreshing = false,
                            error        = result.exception,
                            isLive       = false,
                        )
                    }
                }
            }
            ApiResult.Loading -> Unit
        }
    }


    /** Caches daily history points to avoid redundant network calls during prediction. */
    private var cachedDailyHistory: List<HistoricalDataPoint> = emptyList()

    private fun loadChartData(symbol: String, period: ChartPeriod, isSilent: Boolean = false) {
        if (!isSilent) {
            _uiState.update { it.copy(isChartLoading = true) }
        }
        
        // Define interval and range based on selected period
        val (yahooInterval, yahooRange) = when (period) {
            ChartPeriod.FIVE_MINUTES -> Pair("5m", "1d")
            ChartPeriod.ONE_HOUR -> Pair("1h", "1wk")
            ChartPeriod.ONE_WEEK -> Pair("15m", "5d")
            ChartPeriod.ONE_MONTH -> Pair("1d", "1mo")
            ChartPeriod.THREE_MONTHS -> Pair("1d", "3mo")
            ChartPeriod.SIX_MONTHS -> Pair("1d", "6mo")
            ChartPeriod.ONE_YEAR -> Pair("1d", "1y")
            ChartPeriod.FIVE_YEARS -> Pair("1d", "5y")
        }

        viewModelScope.launch {
            val result = getYahooChartDataUseCase(
                symbol = symbol,
                interval = yahooInterval,
                range = yahooRange
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isChartLoading = false, chartPoints = result.data) }
                }
                else -> {
                    if (!isSilent) {
                        _uiState.update { it.copy(isChartLoading = false, chartPoints = emptyList()) }
                    }
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
        loadChartData(_uiState.value.symbol, period, isSilent = false)
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
        val quote = state.quote ?: return
        val currentPrice = quote.lastPrice
        val symbol = state.symbol.ifBlank { return }

        verificationJob?.cancel()
        _uiState.update {
            it.copy(
                isPredicting = true,
                predictionError = null,
                predictionResult = null,
                isCountdownActive = false,
                predictionCountdownSeconds = 0,
            )
        }

        viewModelScope.launch {
            val result = getPricePredictionUseCase(
                symbol        = symbol,
                companyName   = quote.companyName,
                currentPrice  = currentPrice,
                change        = quote.change,
                changePercent = quote.changePercent,
                dailyHistory  = cachedDailyHistory,
            )

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isPredicting = false,
                            prediction = result.data,
                            predictionError = null,
                            priceAtPrediction = currentPrice,
                            predictionResult = null,
                        )
                    }
                    startVerificationCountdown(symbol, result.data, currentPrice)
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

    private fun startVerificationCountdown(
        symbol: String,
        prediction: PricePrediction,
        priceAtPrediction: Double,
    ) {
        verificationJob?.cancel()
        verificationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCountdownActive = true,
                    predictionCountdownSeconds = VERIFICATION_SECONDS,
                    predictionResult = null,
                )
            }

            // Tick every second
            for (remaining in VERIFICATION_SECONDS - 1 downTo 0) {
                delay(1_000L)
                _uiState.update { it.copy(predictionCountdownSeconds = remaining) }
            }

            // Time's up — fetch the actual price
            _uiState.update { it.copy(isCountdownActive = false) }

            val quoteResult = getStockQuoteUseCase(symbol)
            if (quoteResult is ApiResult.Success) {
                val actualPrice = quoteResult.data.lastPrice
                val priceError = actualPrice - prediction.targetPrice
                val errorPct = if (priceAtPrediction != 0.0) {
                    abs(actualPrice - prediction.targetPrice) / priceAtPrediction * 100.0
                } else 0.0

                val actualDirection = when {
                    actualPrice > priceAtPrediction + 0.01 -> PredictionDirection.UP
                    actualPrice < priceAtPrediction - 0.01 -> PredictionDirection.DOWN
                    else -> PredictionDirection.SIDEWAYS
                }
                val directionCorrect = prediction.direction == actualDirection ||
                    (prediction.direction == PredictionDirection.SIDEWAYS && abs(actualPrice - priceAtPrediction) < priceAtPrediction * 0.001)
                val withinRange = actualPrice in prediction.targetLow..prediction.targetHigh

                val grade = when {
                    withinRange && directionCorrect -> "Excellent"
                    directionCorrect && errorPct < 0.15 -> "Good"
                    directionCorrect -> "Fair"
                    errorPct < 0.1 -> "Fair"
                    else -> "Off Target"
                }

                _uiState.update {
                    it.copy(
                        predictionResult = PredictionResult(
                            prediction = prediction,
                            priceAtPrediction = priceAtPrediction,
                            actualPrice = actualPrice,
                            priceError = priceError,
                            errorPercent = errorPct,
                            directionCorrect = directionCorrect,
                            withinRange = withinRange,
                            grade = grade,
                        ),
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        verificationJob?.cancel()
    }
}
