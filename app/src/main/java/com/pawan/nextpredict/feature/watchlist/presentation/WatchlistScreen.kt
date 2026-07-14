package com.pawan.nextpredict.feature.watchlist.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawan.nextpredict.core.designsystem.component.*
import com.pawan.nextpredict.domain.model.WatchlistStock

@Composable
fun WatchlistScreen(
    onStockClick: (String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Create watchlist dialog
    if (uiState.showCreateDialog) {
        CreateWatchlistDialog(
            name = uiState.newWatchlistName,
            onNameChange = viewModel::onNameChange,
            onConfirm = viewModel::createWatchlist,
            onDismiss = viewModel::dismissCreateDialog,
        )
    }

    Scaffold(
        topBar = {
            NextPredictTopBar(
                title = "Watchlist",
                actions = {
                    IconButton(onClick = viewModel::showCreateDialog) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create watchlist",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (uiState.isLoading) {
                LoadingView()
                return@Scaffold
            }

            if (uiState.watchlists.isEmpty()) {
                EmptyView(
                    icon = Icons.Default.BookmarkBorder,
                    title = "No Watchlists Yet",
                    message = "Create a watchlist to track your favourite stocks.",
                    action = {
                        Button(onClick = viewModel::showCreateDialog) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Watchlist")
                        }
                    },
                )
                return@Scaffold
            }

            // Watchlist tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.watchlists.indexOfFirst {
                    it.id == uiState.selectedWatchlistId
                }.coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
            ) {
                uiState.watchlists.forEach { watchlist ->
                    Tab(
                        selected = watchlist.id == uiState.selectedWatchlistId,
                        onClick = { viewModel.selectWatchlist(watchlist.id) },
                        text = {
                            Text(
                                text = watchlist.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (watchlist.id == uiState.selectedWatchlistId)
                                        FontWeight.SemiBold else FontWeight.Normal,
                                ),
                            )
                        },
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp,
            )

            // Items
            if (uiState.items.isEmpty()) {
                EmptyView(
                    icon = Icons.Default.BookmarkAdd,
                    title = "Empty Watchlist",
                    message = "Search for a stock and tap ☆ to add it here.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(uiState.items, key = { it.symbol }) { stock ->
                        WatchlistStockItem(
                            stock = stock,
                            onClick = { onStockClick(stock.symbol) },
                            onRemove = { viewModel.removeStock(stock.symbol) },
                        )
                        AppDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistStockItem(
    stock: WatchlistStock,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove from Watchlist?") },
            text = { Text("Remove ${stock.symbol} from this watchlist?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showRemoveConfirm = false
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            },
        )
    }

    StockListItem(
        symbol = stock.symbol,
        companyName = stock.companyName,
        lastPrice = stock.lastPrice,
        change = stock.change,
        changePercent = stock.changePercent,
        onClick = onClick,
        trailingContent = {
            IconButton(
                onClick = { showRemoveConfirm = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun CreateWatchlistDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Watchlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("e.g. Swing Trades, Long Term…") },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
