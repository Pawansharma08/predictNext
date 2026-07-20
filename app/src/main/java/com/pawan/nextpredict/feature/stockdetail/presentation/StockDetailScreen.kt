package com.pawan.nextpredict.feature.stockdetail.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.onSizeChanged
import com.pawan.nextpredict.core.designsystem.component.*
import com.pawan.nextpredict.core.designsystem.theme.extendedColors
import com.pawan.nextpredict.core.util.isGain
import com.pawan.nextpredict.core.util.toChangePercentString
import com.pawan.nextpredict.core.util.toChangeString
import com.pawan.nextpredict.core.util.toPriceString
import com.pawan.nextpredict.core.util.toVolumeString
import com.pawan.nextpredict.domain.model.PredictionResult
import com.pawan.nextpredict.domain.model.PricePrediction
import com.pawan.nextpredict.domain.model.PredictionDirection
import com.pawan.nextpredict.domain.model.StockQuote

@Composable
fun StockDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    onOptionChainClick: () -> Unit,
    onFullScreenChartClick: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(symbol) {
        viewModel.loadStock(symbol)
    }

    Scaffold(
        topBar = {
            StockDetailTopBar(
                symbol = symbol,
                isInWatchlist = uiState.isInWatchlist,
                onBack = onBack,
                onWatchlistToggle = viewModel::toggleWatchlist,
            )
        },
        floatingActionButton = {
            if (uiState.quote != null) {
                FloatingActionButton(
                    onClick = onOptionChainClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Option Chain",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingView(modifier = Modifier.padding(paddingValues))

            uiState.error != null && uiState.quote == null -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                exception = uiState.error,
                onRetry = { viewModel.loadStock(symbol) },
            )

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues),
                ) {
                    uiState.quote?.let { quote ->
                        StockDetailContent(
                            quote = quote,
                            selectedPeriod = uiState.selectedChartPeriod,
                            onPeriodSelected = viewModel::selectChartPeriod,
                            chartPoints = uiState.chartPoints,
                            isChartLoading = uiState.isChartLoading,
                            isLive = uiState.isLive,
                            lastUpdatedAt = uiState.lastUpdatedAt,
                            prediction = uiState.prediction,
                            isPredicting = uiState.isPredicting,
                            predictionError = uiState.predictionError,
                            countdownSeconds = uiState.predictionCountdownSeconds,
                            isCountdownActive = uiState.isCountdownActive,
                            predictionResult = uiState.predictionResult,
                            onPredictClick = viewModel::requestPrediction,
                            onFullScreenChartClick = onFullScreenChartClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockDetailTopBar(
    symbol: String,
    isInWatchlist: Boolean,
    onBack: () -> Unit,
    onWatchlistToggle: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            IconButton(onClick = onWatchlistToggle) {
                Icon(
                    imageVector = if (isInWatchlist) Icons.Default.BookmarkAdded
                    else Icons.Default.BookmarkBorder,
                    contentDescription = if (isInWatchlist) "Remove from watchlist"
                    else "Add to watchlist",
                    tint = if (isInWatchlist) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun StockDetailContent(
    quote: StockQuote,
    selectedPeriod: ChartPeriod,
    onPeriodSelected: (ChartPeriod) -> Unit,
    chartPoints: List<com.pawan.nextpredict.domain.model.HistoricalDataPoint>,
    isChartLoading: Boolean,
    isLive: Boolean,
    lastUpdatedAt: String,
    prediction: PricePrediction?,
    isPredicting: Boolean,
    predictionError: String?,
    countdownSeconds: Int,
    isCountdownActive: Boolean,
    predictionResult: PredictionResult?,
    onPredictClick: () -> Unit,
    onFullScreenChartClick: () -> Unit,
) {
    val isGain = quote.change.isGain()
    val priceColor = if (isGain) MaterialTheme.extendedColors.gainColor
    else MaterialTheme.extendedColors.lossColor

    var selectedCandleIdx by remember(selectedPeriod) { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        // Price Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Company name + LIVE badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = quote.companyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                    if (isLive) {
                        // Pulsing LIVE indicator
                        val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "live_alpha",
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.extendedColors.gainColor.copy(alpha = 0.12f),
                                    shape = MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        MaterialTheme.extendedColors.gainColor.copy(alpha = alpha),
                                        shape = MaterialTheme.shapes.small,
                                    )
                            )
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.extendedColors.gainColor,
                            )
                        }
                    }
                }
                if (isLive && lastUpdatedAt.isNotBlank()) {
                    Text(
                        text = "Updated $lastUpdatedAt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = quote.lastPrice.toPriceString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = priceColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = quote.change.toChangeString(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = priceColor,
                    )
                    Text(
                        text = "(${quote.changePercent.toChangePercentString()})",
                        style = MaterialTheme.typography.titleSmall,
                        color = priceColor,
                    )
                }
            }
        }

        // Chart Period Selector
        item {
            ChartPeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = onPeriodSelected,
            )
        }

        // Chart Area
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isChartLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                } else if (chartPoints.isEmpty()) {
                    Text(
                        text = "No chart data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val displayIdx = (selectedCandleIdx ?: chartPoints.lastIndex).coerceIn(0, chartPoints.lastIndex)
                    val displayCandle = chartPoints.getOrNull(displayIdx)
                    val displayDateStr = displayCandle?.let {
                        formatCandleTimestamp(it.date, selectedPeriod)
                    } ?: ""

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (displayCandle != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayDateStr,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OhlcItem(label = "O", value = displayCandle.open)
                                    OhlcItem(label = "H", value = displayCandle.high)
                                    OhlcItem(label = "L", value = displayCandle.low)
                                    OhlcItem(label = "C", value = displayCandle.close)
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            CandlestickChart(
                                points         = chartPoints,
                                selectedPeriod = selectedPeriod,
                                predictionLow  = prediction?.targetLow,
                                predictionHigh = prediction?.targetHigh,
                                predictionTarget = prediction?.targetPrice,
                                direction      = prediction?.direction,
                                selectedCandleIdx = selectedCandleIdx,
                                onSelectedCandleIdxChanged = { selectedCandleIdx = it },
                                showAxes       = false,
                                modifier       = Modifier.fillMaxSize()
                            )
                            
                            IconButton(
                                onClick = onFullScreenChartClick,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.small
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Expand Fullscreen",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Key Statistics
        item {
            KeyStatsSection(quote = quote)
        }

        // Separator
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Circuit Limits
        item {
            CircuitLimitsSection(quote = quote)
        }

        // Separator
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Technical Analysis Prediction
        item {
            PredictionSection(
                prediction = prediction,
                isPredicting = isPredicting,
                predictionError = predictionError,
                countdownSeconds = countdownSeconds,
                isCountdownActive = isCountdownActive,
                predictionResult = predictionResult,
                onPredictClick = onPredictClick,
            )
        }
    }
}

@Composable
private fun ChartPeriodSelector(
    selectedPeriod: ChartPeriod,
    onPeriodSelected: (ChartPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChartPeriod.values().forEach { period ->
            val isSelected = period == selectedPeriod
            FilterChip(
                selected = isSelected,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            )
        }
    }
}

@Composable
private fun KeyStatsSection(quote: StockQuote) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "Key Statistics",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(12.dp))

        val stats = listOf(
            "Open" to quote.open.toPriceString(),
            "Prev Close" to quote.previousClose.toPriceString(),
            "High" to quote.high.toPriceString(),
            "Low" to quote.low.toPriceString(),
            "VWAP" to quote.vwap.toPriceString(),
            "Volume" to quote.volume.toVolumeString(),
            "52W High" to quote.yearHigh.toPriceString(),
            "52W Low" to quote.yearLow.toPriceString(),
            "Face Value" to "₹${quote.faceValue}",
        )

        stats.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (label, value) ->
                    StatItem(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            AppDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CircuitLimitsSection(quote: StockQuote) {
    if (quote.upperCircuit == 0.0 && quote.lowerCircuit == 0.0) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Circuit Limits",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Lower Circuit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = quote.lowerCircuit.toPriceString(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.extendedColors.lossColor,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Upper Circuit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = quote.upperCircuit.toPriceString(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.extendedColors.gainColor,
                    )
                }
            }
        }
    }
}@Composable
fun CandlestickChart(
    points: List<com.pawan.nextpredict.domain.model.HistoricalDataPoint>,
    selectedPeriod: ChartPeriod,
    predictionLow: Double? = null,
    predictionHigh: Double? = null,
    predictionTarget: Double? = null,
    direction: PredictionDirection? = null,
    selectedCandleIdx: Int? = null,
    onSelectedCandleIdxChanged: (Int?) -> Unit,
    showAxes: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedPeriod) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val density = androidx.compose.ui.platform.LocalDensity.current

    // Zoom and pan states
    val minSlotWidth = with(density) { 4.dp.toPx() }
    val maxSlotWidth = with(density) { 60.dp.toPx() }
    var slotWidth by remember { mutableStateOf(with(density) { 15.dp.toPx() }) }
    var scrollOffset by remember { mutableStateOf(0f) }
    var canvasWidth by remember { mutableStateOf(0f) }

    // Reset scrollOffset when points or canvas size changes
    LaunchedEffect(points, canvasWidth, showAxes) {
        if (canvasWidth > 0f && points.isNotEmpty()) {
            val paddingRight = if (showAxes) 100f else 0f
            val chartW = canvasWidth - paddingRight
            val totalWidth = points.size * slotWidth
            scrollOffset = (totalWidth - chartW).coerceAtLeast(0f)
        }
    }

    // Determine the visible points based on slotWidth and scrollOffset
    val visiblePoints = remember(points, scrollOffset, slotWidth, canvasWidth, showAxes) {
        val paddingRight = if (showAxes) 100f else 0f
        val chartW = canvasWidth - paddingRight
        if (chartW <= 0f || points.isEmpty()) {
            points
        } else {
            val firstVisibleIdx = (scrollOffset / slotWidth).toInt().coerceIn(0, points.lastIndex)
            val lastVisibleIdx = ((scrollOffset + chartW) / slotWidth).toInt().coerceIn(0, points.lastIndex)
            points.subList(firstVisibleIdx, lastVisibleIdx + 1)
        }
    }

    val gainColor  = MaterialTheme.extendedColors.gainColor
    val lossColor  = MaterialTheme.extendedColors.lossColor
    val gridColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor  = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    // Calculate price bounds for visible points to implement "Auto Adjust" scaling
    val allPrices = remember(visiblePoints, direction, predictionLow, predictionHigh, predictionTarget) {
        buildList {
            visiblePoints.forEach { add(it.high); add(it.low) }
            if (direction == PredictionDirection.DOWN) {
                predictionLow?.let { add(it) }
            }
            if (direction == PredictionDirection.UP) {
                predictionHigh?.let { add(it) }
            }
            predictionTarget?.let { add(it) }
        }
    }
    val maxPrice   = allPrices.maxOrNull() ?: 1.0
    val minPrice   = allPrices.minOrNull() ?: 0.0
    val priceRange = (maxPrice - minPrice).takeIf { it > 0 } ?: 1.0

    Canvas(
        modifier = modifier
            .onSizeChanged { size ->
                canvasWidth = size.width.toFloat()
            }
            .pointerInput(points, showAxes) {
                detectTransformGestures { centroid, pan, zoomAmount, _ ->
                    val oldSlotWidth = slotWidth
                    slotWidth = (slotWidth * zoomAmount).coerceIn(minSlotWidth, maxSlotWidth)
                    val scale = slotWidth / oldSlotWidth
                    
                    val paddingRight = if (showAxes) 100f else 0f
                    val chartW = canvasWidth - paddingRight
                    val maxScroll = (points.size * slotWidth - chartW).coerceAtLeast(0f)
                    
                    // Centering zoom on gesture centroid
                    scrollOffset = ((scrollOffset + centroid.x) * scale - centroid.x).coerceIn(0f, maxScroll)
                    // Apply drag pan
                    scrollOffset = (scrollOffset - pan.x).coerceIn(0f, maxScroll)

                    // Track selected candle during gestures
                    val touchIdx = ((scrollOffset + centroid.x) / slotWidth).toInt().coerceIn(0, points.lastIndex)
                    onSelectedCandleIdxChanged(touchIdx)
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val paddingRight = if (showAxes) 100f else 0f
        val paddingBottom = if (showAxes) 50f else 0f
        val chartW = w - paddingRight
        val chartH = h - paddingBottom

        val candleWidth = (slotWidth * 0.6f).coerceAtLeast(2f)
        val wickWidth   = (slotWidth * 0.08f).coerceAtLeast(1f)

        fun priceToY(price: Double): Float =
            (chartH * (1.0 - (price - minPrice) / priceRange)).toFloat()

        // ── Grid lines & Y-axis labels ───────────────────────────────────────
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(120, 255, 255, 255) // light gray text
            textSize = 9.dp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }

        repeat(4) { i ->
            val y = chartH * i / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end   = Offset(chartW, y),
                strokeWidth = 1.dp.toPx(),
            )

            if (showAxes) {
                val priceAtGrid = maxPrice - (i / 3f) * priceRange
                drawContext.canvas.nativeCanvas.drawText(
                    "₹${"%.2f".format(priceAtGrid)}",
                    w - 10f,
                    y + 12f,
                    textPaint
                )
            }
        }

        // Calculate indices of visible candles
        val firstVisibleIdx = (scrollOffset / slotWidth).toInt().coerceIn(0, points.lastIndex)
        val lastVisibleIdx = ((scrollOffset + chartW) / slotWidth).toInt().coerceIn(0, points.lastIndex)

        // ── Candles ──────────────────────────────────────────────────────────
        for (idx in firstVisibleIdx..lastVisibleIdx) {
            val candle = points[idx]
            val centerX = slotWidth * idx + slotWidth / 2f - scrollOffset
            val isBull  = candle.close >= candle.open
            val color   = if (isBull) gainColor else lossColor

            val highY  = priceToY(candle.high)
            val lowY   = priceToY(candle.low)
            val openY  = priceToY(candle.open)
            val closeY = priceToY(candle.close)

            val animatedCloseY = openY + (closeY - openY) * animProgress.value
            val animatedHighY  = openY + (highY - openY) * animProgress.value
            val animatedLowY   = openY + (lowY - openY) * animProgress.value

            // Wick
            drawLine(
                color = color,
                start = Offset(centerX, animatedHighY),
                end   = Offset(centerX, animatedLowY),
                strokeWidth = wickWidth,
            )

            // Body
            val bodyTop    = minOf(openY, animatedCloseY)
            val bodyBottom = maxOf(openY, animatedCloseY)
            val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(2f)
            drawRect(
                color   = color,
                topLeft = Offset(centerX - candleWidth / 2f, bodyTop),
                size    = Size(candleWidth, bodyHeight),
            )
        }

        // ── Prediction overlay: dashed horizontal line (direction-based) ──────
        val showHigh = direction == PredictionDirection.UP && predictionHigh != null
        val showLow  = direction == PredictionDirection.DOWN && predictionLow != null
        val showTarget = predictionTarget != null

        if (showHigh || showLow || showTarget) {
            val dashLen   = 12.dp.toPx()
            val gapLen    = 6.dp.toPx()
            val lineWidth = 1.5.dp.toPx()
            val labelPad  = 4.dp.toPx()
            val paint     = android.graphics.Paint().apply {
                textSize  = with(density) { 9.sp.toPx() }
                isFakeBoldText = true
                isAntiAlias = true
            }

            fun drawDashedHLine(yPos: Float, color: androidx.compose.ui.graphics.Color) {
                var x = 0f
                while (x < chartW - labelPad * 8) {
                    drawLine(
                        color = color,
                        start = Offset(x, yPos),
                        end   = Offset((x + dashLen).coerceAtMost(chartW - labelPad * 8), yPos),
                        strokeWidth = lineWidth,
                    )
                    x += dashLen + gapLen
                }
            }

            fun drawSolidHLine(yPos: Float, color: androidx.compose.ui.graphics.Color) {
                drawLine(
                    color = color,
                    start = Offset(0f, yPos),
                    end   = Offset(chartW - labelPad * 8, yPos),
                    strokeWidth = lineWidth * 1.5f,
                )
            }

            if (showHigh && predictionHigh != null) {
                val yHigh = priceToY(predictionHigh)
                drawDashedHLine(yHigh, gainColor)
                // Label on right edge
                drawContext.canvas.nativeCanvas.apply {
                    paint.color = android.graphics.Color.argb(
                        255,
                        (gainColor.red * 255).toInt(),
                        (gainColor.green * 255).toInt(),
                        (gainColor.blue * 255).toInt(),
                    )
                    drawText("Target High: H ₹${"%.1f".format(predictionHigh)}", chartW - labelPad * 26, yHigh - labelPad, paint)
                }
            }

            if (showLow && predictionLow != null) {
                val yLow = priceToY(predictionLow)
                drawDashedHLine(yLow, lossColor)
                drawContext.canvas.nativeCanvas.apply {
                    paint.color = android.graphics.Color.argb(
                        255,
                        (lossColor.red * 255).toInt(),
                        (lossColor.green * 255).toInt(),
                        (lossColor.blue * 255).toInt(),
                    )
                    drawText("Target Low: L ₹${"%.1f".format(predictionLow)}", chartW - labelPad * 26, yLow + labelPad * 3, paint)
                }
            }

            if (showTarget && predictionTarget != null) {
                val yTarget = priceToY(predictionTarget)
                val targetColor = androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
                drawSolidHLine(yTarget, targetColor)
                drawContext.canvas.nativeCanvas.apply {
                    paint.color = android.graphics.Color.argb(
                        255,
                        (targetColor.red * 255).toInt(),
                        (targetColor.green * 255).toInt(),
                        (targetColor.blue * 255).toInt(),
                    )
                    drawText("Predicted: ₹${"%.1f".format(predictionTarget)}", chartW - labelPad * 26, yTarget - labelPad, paint)
                }
            }
        }

        // ── Crosshair line ───────────────────────────────────────────────────
        selectedCandleIdx?.let { idx ->
            if (idx in firstVisibleIdx..lastVisibleIdx) {
                val centerX = slotWidth * idx + slotWidth / 2f - scrollOffset
                // Vertical crosshair line (dashed)
                drawLine(
                    color = primaryColor.copy(alpha = 0.4f),
                    start = Offset(centerX, 0f),
                    end   = Offset(centerX, chartH),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f), 0f
                    )
                )
                
                // Highlight circle at closing price of the selected candle
                val candle = points[idx]
                val closeY = priceToY(candle.close)
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(centerX, closeY)
                )
            }
        }

        // ── Red Live Price Tag (when showAxes is true) ────────────────────────
        if (showAxes && points.isNotEmpty()) {
            val currentPrice = points.last().close
            val yCurrent = priceToY(currentPrice)
            if (yCurrent in 0f..chartH) {
                // Red horizontal line
                drawLine(
                    color = androidx.compose.ui.graphics.Color.Red,
                    start = Offset(0f, yCurrent),
                    end   = Offset(chartW, yCurrent),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Draw red box tag on right axis
                val tagW = 75f
                val tagH = 30f
                drawRect(
                    color = androidx.compose.ui.graphics.Color.Red,
                    topLeft = Offset(chartW + 5f, yCurrent - tagH / 2f),
                    size = Size(tagW, tagH)
                )

                val tagTextPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "%.2f".format(currentPrice),
                    chartW + 5f + tagW / 2f,
                    yCurrent + 10f,
                    tagTextPaint
                )
            }
        }

        // ── X-axis date/time labels ──────────────────────────────────────────
        if (showAxes && points.isNotEmpty()) {
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(120, 255, 255, 255)
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            val step = ((lastVisibleIdx - firstVisibleIdx) / 3).coerceAtLeast(5)
            for (idx in firstVisibleIdx..lastVisibleIdx step step) {
                val candle = points.getOrNull(idx) ?: continue
                val centerX = slotWidth * idx + slotWidth / 2f - scrollOffset
                if (centerX in 0f..chartW) {
                    val label = formatCandleTimestamp(candle.date, selectedPeriod)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        centerX,
                        h - 10f,
                        labelPaint
                    )
                }
            }
        }
    }
}

// Keep the old line chart as fallback reference (unused but available)
@Suppress("unused")
@Composable
private fun InteractiveLineChart(
    points: List<com.pawan.nextpredict.domain.model.HistoricalDataPoint>,
    isGain: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val closePrices = points.map { it.close }
    val maxPrice = (closePrices.maxOrNull() ?: 1.0) * 1.002
    val minPrice = (closePrices.minOrNull() ?: 0.0) * 0.998
    val priceRange = maxPrice - minPrice

    val gainColor = MaterialTheme.extendedColors.gainColor
    val lossColor = MaterialTheme.extendedColors.lossColor
    val strokeColor = if (isGain) gainColor else lossColor

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()
        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val priceRatio = if (priceRange != 0.0) ((pt.close - minPrice) / priceRange) else 0.5
            val y = height - (priceRatio.toFloat() * height)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (i == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(strokeColor.copy(alpha = 0.2f), strokeColor.copy(alpha = 0.0f)),
                startY = 0f, endY = height
            )
        )
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}

// ─── Technical Analysis Prediction Section ───────────────────────────────────

@Composable
private fun PredictionSection(
    prediction: PricePrediction?,
    isPredicting: Boolean,
    predictionError: String?,
    countdownSeconds: Int,
    isCountdownActive: Boolean,
    predictionResult: PredictionResult?,
    onPredictClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section header
        Text(
            text = "5-Minute Price Prediction",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Predict button
        Button(
            onClick = onPredictClick,
            enabled = !isPredicting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (isPredicting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing candles…")
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (prediction == null) "🔮 Predict Next 5 Min" else "🔄 Re-Predict")
            }
        }

        // Error state
        if (predictionError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.extendedColors.lossColor.copy(alpha = 0.1f),
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.extendedColors.lossColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = predictionError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extendedColors.lossColor,
                    )
                }
            }
        }

        // Prediction result card
        if (prediction != null) {
            PredictionCard(prediction = prediction)
        }

        // Countdown timer while waiting for verification
        if (isCountdownActive && prediction != null) {
            CountdownTimerCard(countdownSeconds = countdownSeconds)
        }

        // Accuracy result after 5 minutes
        if (predictionResult != null) {
            PredictionAccuracyCard(result = predictionResult)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PredictionCard(prediction: PricePrediction) {
    val (directionIcon, directionLabel, cardColor) = when (prediction.direction) {
        PredictionDirection.UP -> Triple(
            Icons.Default.TrendingUp,
            "HIGH ↑",
            MaterialTheme.extendedColors.gainColor,
        )
        PredictionDirection.DOWN -> Triple(
            Icons.Default.TrendingDown,
            "LOW ↓",
            MaterialTheme.extendedColors.lossColor,
        )
        PredictionDirection.SIDEWAYS -> Triple(
            Icons.Default.TrendingFlat,
            "SIDEWAYS →",
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.08f),
        ),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = cardColor.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row (Direction & Confidence Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = null,
                        tint = cardColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Direction: $directionLabel",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = cardColor,
                    )
                }

                // Confidence Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = cardColor.copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${prediction.confidence}% Confident",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = cardColor,
                    )
                }
            }

            // Price Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Predicted Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "₹${"%.2f".format(prediction.targetPrice)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target Range",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "₹${"%.2f".format(prediction.targetLow)} - ₹${"%.2f".format(prediction.targetHigh)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Divider before time metrics
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Prediction Timeline & Target Arrival Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Analyzed At",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = prediction.generatedAt,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = cardColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Expected Arrival",
                            style = MaterialTheme.typography.labelSmall,
                            color = cardColor
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = prediction.targetTime,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = cardColor
                    )
                }
            }

            // Divider before reasoning
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Technical Reasoning Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Technical Reasoning",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = prediction.reasoning,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─── Countdown Timer ─────────────────────────────────────────────────────────

@Composable
private fun CountdownTimerCard(countdownSeconds: Int) {
    val minutes = countdownSeconds / 60
    val seconds = countdownSeconds % 60
    val progress = 1f - (countdownSeconds.toFloat() / (5 * 60))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Verifying in…",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "%d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )

            Text(
                text = "Waiting for 5 minutes to compare predicted vs actual price…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Prediction Accuracy Result Card ─────────────────────────────────────────

@Composable
private fun PredictionAccuracyCard(result: PredictionResult) {
    val gradeColor = when (result.grade) {
        "Excellent" -> MaterialTheme.extendedColors.gainColor
        "Good" -> MaterialTheme.extendedColors.gainColor
        "Fair" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.extendedColors.lossColor
    }
    val gradeEmoji = when (result.grade) {
        "Excellent" -> "🎯"
        "Good" -> "✅"
        "Fair" -> "🔶"
        else -> "❌"
    }
    val directionEmoji = if (result.directionCorrect) "✅" else "❌"
    val rangeEmoji = if (result.withinRange) "✅" else "❌"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = gradeColor.copy(alpha = 0.08f),
        ),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = gradeColor.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: Grade badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = gradeColor,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "Prediction Result",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = gradeColor.copy(alpha = 0.18f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "$gradeEmoji ${result.grade}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = gradeColor,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp,
            )

            // Price comparison: Predicted vs Actual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Price at Prediction",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "₹${"%.2f".format(result.priceAtPrediction)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Predicted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "₹${"%.2f".format(result.prediction.targetPrice)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Actual",
                        style = MaterialTheme.typography.labelSmall,
                        color = gradeColor,
                    )
                    Text(
                        text = "₹${"%.2f".format(result.actualPrice)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = gradeColor,
                    )
                }
            }

            // Visual bar: predicted vs actual position between low and high
            PredictionVsActualBar(result = result, gradeColor = gradeColor)

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp,
            )

            // Error & Checks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Price Error",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val errorSign = if (result.priceError >= 0) "+" else ""
                    Text(
                        text = "$errorSign${"%.2f".format(result.priceError)} (${"%.2f".format(result.errorPercent)}%)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (result.errorPercent < 0.15) MaterialTheme.extendedColors.gainColor
                                else MaterialTheme.extendedColors.lossColor,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$directionEmoji Direction: ${if (result.directionCorrect) "Correct" else "Wrong"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$rangeEmoji Within Range: ${if (result.withinRange) "Yes" else "No"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun PredictionVsActualBar(result: PredictionResult, gradeColor: androidx.compose.ui.graphics.Color) {
    val low = result.prediction.targetLow
    val high = result.prediction.targetHigh
    val range = high - low
    if (range <= 0) return

    val predictedFraction = ((result.prediction.targetPrice - low) / range).toFloat().coerceIn(0f, 1f)
    val actualFraction = ((result.actualPrice - low) / range).toFloat().coerceIn(-0.1f, 1.1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Target Range: ₹${"%.2f".format(low)} — ₹${"%.2f".format(high)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        ) {
            // Track
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            ) {
                val trackY = size.height / 2
                val trackH = 8.dp.toPx()
                val cornerR = trackH / 2

                // Full range bar
                drawRoundRect(
                    color = gradeColor.copy(alpha = 0.12f),
                    topLeft = Offset(0f, trackY - trackH / 2),
                    size = Size(size.width, trackH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR),
                )

                // Predicted marker (blue diamond)
                val predX = (predictedFraction * size.width).coerceIn(6f, size.width - 6f)
                val diamondSize = 10.dp.toPx()
                val diamondPath = Path().apply {
                    moveTo(predX, trackY - diamondSize)
                    lineTo(predX + diamondSize * 0.7f, trackY)
                    lineTo(predX, trackY + diamondSize)
                    lineTo(predX - diamondSize * 0.7f, trackY)
                    close()
                }
                drawPath(diamondPath, color = androidx.compose.ui.graphics.Color(0xFF2196F3))

                // Actual marker (circle)
                val actX = (actualFraction * size.width).coerceIn(6f, size.width - 6f)
                drawCircle(
                    color = gradeColor,
                    radius = 7.dp.toPx(),
                    center = Offset(actX, trackY),
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(actX, trackY),
                )
            }
        }
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(1.dp),
                        ),
                )
                Text(
                    text = "Predicted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = gradeColor, shape = androidx.compose.foundation.shape.CircleShape),
                )
                Text(
                    text = "Actual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun OhlcItem(label: String, value: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = "₹${"%.2f".format(value)}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatCandleTimestamp(dateStr: String, period: ChartPeriod): String {
    if (dateStr.length < 16) return dateStr
    val datePart = dateStr.substring(0, 10) // "yyyy-MM-dd"
    val timePart = dateStr.substring(11, 16) // "HH:mm"
    
    return when (period) {
        ChartPeriod.FIVE_MINUTES, ChartPeriod.ONE_HOUR -> {
            val parts = datePart.split("-")
            if (parts.size == 3) {
                val day = parts[2]
                val monthNum = parts[1].toIntOrNull() ?: 1
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthName = months.getOrElse(monthNum - 1) { "" }
                "$day $monthName, $timePart"
            } else {
                timePart
            }
        }
        else -> {
            val parts = datePart.split("-")
            if (parts.size == 3) {
                val year = parts[0].substring(2)
                val monthNum = parts[1].toIntOrNull() ?: 1
                val day = parts[2]
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthName = months.getOrElse(monthNum - 1) { "" }
                "$day $monthName '$year"
            } else {
                datePart
            }
        }
    }
}
