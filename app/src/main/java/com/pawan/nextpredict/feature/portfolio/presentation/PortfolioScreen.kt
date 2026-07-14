package com.pawan.nextpredict.feature.portfolio.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawan.nextpredict.core.designsystem.component.EmptyView
import com.pawan.nextpredict.core.designsystem.component.NextPredictTopBar

@Composable
fun PortfolioScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Holdings", "Orders", "Positions")

    Scaffold(
        topBar = {
            NextPredictTopBar(title = "Portfolio")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == index)
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

            when (selectedTab) {
                0 -> EmptyView(
                    icon = Icons.Default.AccountBalance,
                    title = "No Holdings",
                    message = "Your portfolio holdings will appear here.\nThis is a local-only portfolio tracker.",
                )
                1 -> EmptyView(
                    icon = Icons.Default.Receipt,
                    title = "No Orders",
                    message = "Your order history will appear here.",
                )
                2 -> EmptyView(
                    icon = Icons.Default.TrendingUp,
                    title = "No Positions",
                    message = "Your open positions will appear here.",
                )
            }
        }
    }
}
