package com.pawan.nextpredict.feature.home.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawan.nextpredict.core.designsystem.component.*
import com.pawan.nextpredict.core.designsystem.theme.extendedColors
import com.pawan.nextpredict.domain.model.Index
import com.pawan.nextpredict.domain.model.MarketStatus
import com.pawan.nextpredict.domain.model.Stock

@Composable
fun HomeScreen(
    onStockClick: (String) -> Unit,
    onViewAllGainers: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToStock -> onStockClick(effect.symbol)
                HomeEffect.NavigateToGainers -> onViewAllGainers()
                HomeEffect.NavigateToLosers -> onViewAllGainers()
                HomeEffect.NavigateToActive -> onViewAllGainers()
            }
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(onSettingsClick = onSettingsClick)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    HomeShimmerLayout()
                }
            }

            uiState.error != null && !uiState.hasData -> {
                ErrorView(
                    modifier = Modifier.padding(paddingValues),
                    exception = uiState.error,
                    onRetry = { viewModel.onEvent(HomeEvent.Refresh) },
                )
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.onEvent(HomeEvent.Refresh) },
                    modifier = Modifier.padding(paddingValues),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        // Market Status Banner
                        item {
                            uiState.marketStatus?.let { status ->
                                MarketStatusBanner(status = status)
                            }
                        }

                        // Index Cards (horizontal scroll)
                        if (uiState.indices.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Market Overview")
                                IndexCardsRow(
                                    indices = uiState.indices,
                                    onIndexClick = {},
                                )
                            }
                        }

                        // Top Gainers
                        if (uiState.topGainers.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Top Gainers",
                                    onViewAll = { viewModel.onEvent(HomeEvent.ViewAllGainersClicked) },
                                )
                            }
                            items(
                                items = uiState.topGainers.take(5),
                                key = { it.symbol },
                            ) { stock ->
                                StockListItem(
                                    symbol = stock.symbol,
                                    companyName = stock.companyName,
                                    lastPrice = stock.lastPrice,
                                    change = stock.change,
                                    changePercent = stock.changePercent,
                                    onClick = {
                                        viewModel.onEvent(HomeEvent.StockClicked(stock.symbol))
                                    },
                                )
                                AppDivider()
                            }
                        }

                        // Top Losers
                        if (uiState.topLosers.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Top Losers",
                                    onViewAll = { viewModel.onEvent(HomeEvent.ViewAllLosersClicked) },
                                )
                            }
                            items(
                                items = uiState.topLosers.take(5),
                                key = { "loser_${it.symbol}" },
                            ) { stock ->
                                StockListItem(
                                    symbol = stock.symbol,
                                    companyName = stock.companyName,
                                    lastPrice = stock.lastPrice,
                                    change = stock.change,
                                    changePercent = stock.changePercent,
                                    onClick = {
                                        viewModel.onEvent(HomeEvent.StockClicked(stock.symbol))
                                    },
                                )
                                AppDivider()
                            }
                        }

                        // Most Active
                        if (uiState.mostActive.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Most Active",
                                    onViewAll = { viewModel.onEvent(HomeEvent.ViewAllActiveClicked) },
                                )
                            }
                            items(
                                items = uiState.mostActive.take(5),
                                key = { "active_${it.symbol}" },
                            ) { stock ->
                                StockListItem(
                                    symbol = stock.symbol,
                                    companyName = stock.companyName,
                                    lastPrice = stock.lastPrice,
                                    change = stock.change,
                                    changePercent = stock.changePercent,
                                    onClick = {
                                        viewModel.onEvent(HomeEvent.StockClicked(stock.symbol))
                                    },
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

@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "NextPredict",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "BSE/NSE Live Markets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun MarketStatusBanner(status: MarketStatus) {
    val (bgColor, textColor, icon) = if (status.isOpen) {
        Triple(
            MaterialTheme.extendedColors.gainColor.copy(alpha = 0.12f),
            MaterialTheme.extendedColors.gainColor,
            Icons.Default.CheckCircle,
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Schedule,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = status.statusMessage,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = textColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = status.tradeDate,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun IndexCardsRow(
    indices: List<Index>,
    onIndexClick: (Index) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(indices, key = { it.name }) { index ->
            IndexCard(
                name = index.name,
                value = index.lastPrice,
                change = index.change,
                changePercent = index.changePercent,
                onClick = { onIndexClick(index) },
            )
        }
    }
}
