package com.pawan.nextpredict.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ─── Dark Color Scheme ────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = TextOnGreen,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenLight,

    secondary = GreenLight,
    onSecondary = TextOnGreen,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = GreenLight,

    tertiary = GoldColor,
    onTertiary = Color.Black,

    error = RedPrimary,
    onError = TextOnRed,
    errorContainer = RedContainer,
    onErrorContainer = RedLight,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = DarkBorder,
    outlineVariant = DarkDivider,

    inverseSurface = LightSurface,
    inverseOnSurface = Color.Black,
    inversePrimary = GreenDark,

    surfaceTint = GreenPrimary,
)

// ─── Light Color Scheme ───────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA),
    onPrimaryContainer = Color(0xFF002112),

    secondary = GreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB9F6CA),
    onSecondaryContainer = Color(0xFF002112),

    tertiary = Color(0xFF795900),
    onTertiary = Color.White,

    error = RedDark,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = LightBackground,
    onBackground = Color(0xFF1A1C1E),

    surface = LightSurface,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF44474F),

    outline = LightBorder,
    outlineVariant = LightDivider,
)

// ─── Extra Colors CompositionLocal ────────────────────────────────────────────

data class ExtendedColors(
    val gainColor: Color,
    val lossColor: Color,
    val neutralColor: Color,
    val cardBackground: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val chartGain: Color,
    val chartLoss: Color,
    val chartGainFill: Color,
    val chartLossFill: Color,
    val chartGrid: Color,
    val divider: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        gainColor = GainColor,
        lossColor = LossColor,
        neutralColor = NeutralColor,
        cardBackground = DarkCardBackground,
        shimmerBase = ShimmerBase,
        shimmerHighlight = ShimmerHighlight,
        chartGain = ChartGain,
        chartLoss = ChartLoss,
        chartGainFill = ChartGainFill,
        chartLossFill = ChartLossFill,
        chartGrid = ChartGrid,
        divider = DarkDivider,
    )
}

private val DarkExtendedColors = ExtendedColors(
    gainColor = GainColor,
    lossColor = LossColor,
    neutralColor = NeutralColor,
    cardBackground = DarkCardBackground,
    shimmerBase = ShimmerBase,
    shimmerHighlight = ShimmerHighlight,
    chartGain = ChartGain,
    chartLoss = ChartLoss,
    chartGainFill = ChartGainFill,
    chartLossFill = ChartLossFill,
    chartGrid = ChartGrid,
    divider = DarkDivider,
)

private val LightExtendedColors = ExtendedColors(
    gainColor = GreenDark,
    lossColor = RedDark,
    neutralColor = Color(0xFF6E7079),
    cardBackground = LightCardBackground,
    shimmerBase = Color(0xFFE0E0E0),
    shimmerHighlight = Color(0xFFF5F5F5),
    chartGain = GreenDark,
    chartLoss = RedDark,
    chartGainFill = Color(0x20009624),
    chartLossFill = Color(0x20C62828),
    chartGrid = LightDivider,
    divider = LightDivider,
)

// ─── Theme ────────────────────────────────────────────────────────────────────

@Composable
fun NextPredictTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/**
 * Extension to access extended colors from anywhere in the Compose tree.
 */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
