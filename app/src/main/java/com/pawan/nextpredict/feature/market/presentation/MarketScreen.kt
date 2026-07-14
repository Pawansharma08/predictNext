package com.pawan.nextpredict.feature.market.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawan.nextpredict.core.designsystem.component.*

@Composable
fun MarketScreen(
    onStockClick: (String) -> Unit,
    onIndexClick: (String) -> Unit,
    viewModel: MarketViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            NextPredictTopBar(title = "Market")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Index Tab Row
            ScrollableTabRow(
                selectedTabIndex = viewModel.availableIndices.indexOf(uiState.selectedIndex)
                    .coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
            ) {
                viewModel.availableIndices.forEach { indexName ->
                    Tab(
                        selected = indexName == uiState.selectedIndex,
                        onClick = { viewModel.selectIndex(indexName) },
                        text = {
                            Text(
                                text = indexName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (indexName == uiState.selectedIndex)
                                        FontWeight.SemiBold else FontWeight.Normal
                                ),
                            )
                        },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null && uiState.stocks.isEmpty() -> ErrorView(
                    exception = uiState.error,
                    onRetry = { viewModel.refresh() },
                )
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) {
                            // Header row
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "SYMBOL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "LTP / CHANGE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 0.5.dp,
                                )
                            }

                            items(
                                items = uiState.stocks,
                                key = { it.symbol },
                            ) { stock ->
                                StockListItem(
                                    symbol = stock.symbol,
                                    companyName = stock.companyName,
                                    lastPrice = stock.lastPrice,
                                    change = stock.change,
                                    changePercent = stock.changePercent,
                                    onClick = { onStockClick(stock.symbol) },
                                )
                                AppDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
