package com.pawan.nextpredict.feature.stockdetail.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawan.nextpredict.core.designsystem.theme.extendedColors
import com.pawan.nextpredict.core.util.isGain
import com.pawan.nextpredict.core.util.toChangePercentString
import com.pawan.nextpredict.core.util.toChangeString
import com.pawan.nextpredict.core.util.toPriceString

@Composable
fun FullScreenChartScreen(
    symbol: String,
    onBack: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(symbol) {
        viewModel.loadStock(symbol)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedCandleIdx by remember(uiState.selectedChartPeriod) { mutableStateOf<Int?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C)) // Groww app pure dark background
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Custom Header (Close X, Search, Interval Dropdown, Edit, Indicators, Settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                    }

                    // Interval drop-down
                    Box {
                        TextButton(
                            onClick = { menuExpanded = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text(
                                text = uiState.selectedChartPeriod.label,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            ChartPeriod.values().forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.label) },
                                    onClick = {
                                        viewModel.selectChartPeriod(period)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Drawing",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Functions,
                                contentDescription = "Indicators",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Subheader with stock details
                uiState.quote?.let { quote ->
                    val isGain = quote.change.isGain()
                    val priceColor = if (isGain) MaterialTheme.extendedColors.gainColor
                    else MaterialTheme.extendedColors.lossColor

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${quote.symbol} • NSE",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.LightGray
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = quote.lastPrice.toPriceString(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${quote.change.toChangeString()} (${quote.changePercent.toChangePercentString()})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = priceColor
                            )
                        }
                    }
                }

                // Highlighted inspecting candle details row
                if (uiState.chartPoints.isNotEmpty()) {
                    val displayIdx = (selectedCandleIdx ?: uiState.chartPoints.lastIndex).coerceIn(0, uiState.chartPoints.lastIndex)
                    val displayCandle = uiState.chartPoints.getOrNull(displayIdx)
                    val displayDateStr = displayCandle?.let {
                        formatCandleTimestamp(it.date, uiState.selectedChartPeriod)
                    } ?: ""

                    if (displayCandle != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayDateStr,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Gray
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OhlcItem(label = "O", value = displayCandle.open)
                                OhlcItem(label = "H", value = displayCandle.high)
                                OhlcItem(label = "L", value = displayCandle.low)
                                OhlcItem(label = "C", value = displayCandle.close)
                            }
                        }
                    }
                }

                // Full Screen Chart area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 16.dp, top = 8.dp)
                ) {
                    if (uiState.chartPoints.isNotEmpty()) {
                        CandlestickChart(
                            points = uiState.chartPoints,
                            selectedPeriod = uiState.selectedChartPeriod,
                            predictionLow = uiState.prediction?.targetLow,
                            predictionHigh = uiState.prediction?.targetHigh,
                            direction = uiState.prediction?.direction,
                            selectedCandleIdx = selectedCandleIdx,
                            onSelectedCandleIdxChanged = { selectedCandleIdx = it },
                            showAxes = true, // Render X and Y axes, red current price line, and red tag
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
