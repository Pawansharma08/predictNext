package com.pawan.nextpredict.core.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pawan.nextpredict.core.designsystem.theme.extendedColors

/**
 * Shimmer loading effect modifier.
 *
 * Applies an animated gradient shimmer effect to any composable.
 * Used for skeleton loading states across the app.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val shimmerColors = listOf(
        MaterialTheme.extendedColors.shimmerBase,
        MaterialTheme.extendedColors.shimmerHighlight,
        MaterialTheme.extendedColors.shimmerBase,
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    ).value

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 200f, y = 0f),
        end = Offset(x = translateAnim, y = 0f),
    )

    background(brush)
}

/**
 * Pre-built shimmer boxes for common skeleton layouts.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerEffect()
    )
}

@Composable
fun StockCardShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ShimmerBox(modifier = Modifier.width(120.dp), height = 16.dp)
            ShimmerBox(modifier = Modifier.width(80.dp), height = 16.dp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ShimmerBox(modifier = Modifier.width(80.dp), height = 14.dp)
            ShimmerBox(modifier = Modifier.width(60.dp), height = 14.dp)
        }
    }
}

@Composable
fun IndexCardShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(160.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShimmerBox(modifier = Modifier.width(80.dp), height = 12.dp)
        ShimmerBox(modifier = Modifier.width(120.dp), height = 22.dp)
        ShimmerBox(modifier = Modifier.width(90.dp), height = 14.dp)
    }
}

@Composable
fun HomeShimmerLayout() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Market status shimmer
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            height = 48.dp,
            cornerRadius = 12.dp,
        )

        // Index cards shimmer
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(3) { IndexCardShimmer() }
        }

        // Stock list shimmer
        repeat(5) {
            StockCardShimmer()
        }
    }
}
