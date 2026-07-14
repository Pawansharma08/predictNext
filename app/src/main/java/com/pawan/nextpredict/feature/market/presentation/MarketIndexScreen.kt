package com.pawan.nextpredict.feature.market.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawan.nextpredict.core.designsystem.component.ErrorView
import com.pawan.nextpredict.core.designsystem.component.LoadingView
import com.pawan.nextpredict.core.designsystem.theme.extendedColors
import com.pawan.nextpredict.core.util.isGain
import com.pawan.nextpredict.core.util.toChangePercentString
import com.pawan.nextpredict.core.util.toPriceString
import com.pawan.nextpredict.domain.model.Stock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketIndexScreen(
    indexName: String,
    onStockClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MarketIndexViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(indexName) {
        viewModel.loadIndex(indexName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(indexName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingView(modifier = Modifier.padding(paddingValues))

            uiState.error != null && uiState.stocks.isEmpty() -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                exception = uiState.error,
                onRetry = { viewModel.loadIndex(indexName) }
            )

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.stocks) { stock ->
                        StockConstituentRow(stock = stock, onClick = { onStockClick(stock.symbol) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StockConstituentRow(
    stock: Stock,
    onClick: () -> Unit,
) {
    val isGain = stock.change.isGain()
    val trendColor = if (isGain) MaterialTheme.extendedColors.gainColor
    else MaterialTheme.extendedColors.lossColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stock.companyName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stock.lastPrice.toPriceString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stock.changePercent.toChangePercentString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = trendColor
                    )
                }
            }
        }
    }
}
