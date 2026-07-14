package com.pawan.nextpredict.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * All navigation routes in the app — single source of truth.
 */
sealed class Screen(val route: String) {
    // ─── Splash ───────────────────────────────────────────────────────────────
    data object Splash : Screen("splash")

    // ─── Main Tabs ────────────────────────────────────────────────────────────
    data object Home : Screen("home")
    data object Market : Screen("market")
    data object Search : Screen("search")
    data object Watchlist : Screen("watchlist")
    data object Portfolio : Screen("portfolio")

    // ─── Detail Screens ───────────────────────────────────────────────────────
    data object StockDetail : Screen("stock_detail/{symbol}") {
        fun createRoute(symbol: String) = "stock_detail/$symbol"
        const val ARG_SYMBOL = "symbol"
    }

    data object OptionChain : Screen("option_chain/{symbol}") {
        fun createRoute(symbol: String) = "option_chain/$symbol"
        const val ARG_SYMBOL = "symbol"
    }

    data object MarketIndex : Screen("market_index/{indexName}") {
        fun createRoute(indexName: String) = "market_index/${indexName.replace(" ", "%20")}"
        const val ARG_INDEX_NAME = "indexName"
    }

    // ─── Other Screens ────────────────────────────────────────────────────────
    data object News : Screen("news")
    data object Alerts : Screen("alerts")
    data object Settings : Screen("settings")
}

/**
 * Bottom navigation items.
 */
enum class BottomNavItem(
    val screen: Screen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        screen = Screen.Home,
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    Market(
        screen = Screen.Market,
        title = "Market",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
    ),
    Search(
        screen = Screen.Search,
        title = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    ),
    Watchlist(
        screen = Screen.Watchlist,
        title = "Watchlist",
        selectedIcon = Icons.Filled.BookmarkAdded,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
    ),
    Portfolio(
        screen = Screen.Portfolio,
        title = "Portfolio",
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance,
    ),
}
