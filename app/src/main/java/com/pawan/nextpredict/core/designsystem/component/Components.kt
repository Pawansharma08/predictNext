package com.pawan.nextpredict.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pawan.nextpredict.core.designsystem.theme.extendedColors
import com.pawan.nextpredict.core.util.isGain
import com.pawan.nextpredict.core.util.toChangePercentString
import com.pawan.nextpredict.core.util.toChangeString
import com.pawan.nextpredict.core.util.toPriceString

/**
 * Stock list row — used in watchlist, search results, and movers lists.
 */
@Composable
fun StockListItem(
    symbol: String,
    companyName: String,
    lastPrice: Double,
    change: Double,
    changePercent: Double,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val gainColor = MaterialTheme.extendedColors.gainColor
    val lossColor = MaterialTheme.extendedColors.lossColor
    val priceColor = if (change.isGain()) gainColor else lossColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Symbol + Company Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = companyName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Price + Change
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = lastPrice.toPriceString(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            PriceChangeChip(
                change = change,
                changePercent = changePercent,
            )
        }

        // Optional trailing (e.g., watchlist toggle)
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

/**
 * Compact price change chip: e.g., +1.25%
 */
@Composable
fun PriceChangeChip(
    change: Double,
    changePercent: Double,
    modifier: Modifier = Modifier,
    showAbsoluteValue: Boolean = false,
) {
    val gainColor = MaterialTheme.extendedColors.gainColor
    val lossColor = MaterialTheme.extendedColors.lossColor
    val isGain = change.isGain()
    val color = if (isGain) gainColor else lossColor
    val bgColor = if (isGain) {
        MaterialTheme.extendedColors.gainColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.extendedColors.lossColor.copy(alpha = 0.12f)
    }

    val text = if (showAbsoluteValue) {
        "${changePercent.toChangePercentString()} (${change.toChangeString()})"
    } else {
        changePercent.toChangePercentString()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = color,
        )
    }
}

/**
 * Index summary card — used in home screen for Nifty, BankNifty, etc.
 */
@Composable
fun IndexCard(
    name: String,
    value: Double,
    change: Double,
    changePercent: Double,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val gainColor = MaterialTheme.extendedColors.gainColor
    val lossColor = MaterialTheme.extendedColors.lossColor
    val isGain = change.isGain()
    val priceColor = if (isGain) gainColor else lossColor

    Card(
        modifier = modifier
            .width(160.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardBackground,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value.toPriceString().removePrefix("₹"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = priceColor,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = changePercent.toChangePercentString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = priceColor,
                )
            }
        }
    }
}

/**
 * Section header with optional "View All" action.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onViewAll: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (onViewAll != null) {
            TextButton(onClick = onViewAll) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Top App Bar for screens.
 */
@Composable
fun NextPredictTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

/**
 * Divider with consistent styling.
 */
@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.extendedColors.divider,
        thickness = 0.5.dp,
    )
}
