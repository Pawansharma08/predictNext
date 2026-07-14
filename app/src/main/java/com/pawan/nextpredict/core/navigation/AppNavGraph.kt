package com.pawan.nextpredict.core.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pawan.nextpredict.feature.home.presentation.HomeScreen
import com.pawan.nextpredict.feature.market.presentation.MarketScreen
import com.pawan.nextpredict.feature.market.presentation.MarketIndexScreen
import com.pawan.nextpredict.feature.portfolio.presentation.PortfolioScreen
import com.pawan.nextpredict.feature.search.presentation.SearchScreen
import com.pawan.nextpredict.feature.settings.presentation.SettingsScreen
import com.pawan.nextpredict.feature.stockdetail.presentation.StockDetailScreen
import com.pawan.nextpredict.feature.stockdetail.presentation.OptionChainScreen
import com.pawan.nextpredict.feature.watchlist.presentation.WatchlistScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { it / 4 }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
                    slideOutHorizontally(animationSpec = tween(200)) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                    slideOutHorizontally(animationSpec = tween(200)) { it / 4 }
        },
    ) {
        // ─── Main Tabs ────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onStockClick = { symbol ->
                    navController.navigate(Screen.StockDetail.createRoute(symbol))
                },
                onViewAllGainers = {
                    navController.navigate(Screen.Market.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(Screen.Market.route) {
            MarketScreen(
                onStockClick = { symbol ->
                    navController.navigate(Screen.StockDetail.createRoute(symbol))
                },
                onIndexClick = { indexName ->
                    navController.navigate(Screen.MarketIndex.createRoute(indexName))
                },
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onStockClick = { symbol ->
                    navController.navigate(Screen.StockDetail.createRoute(symbol))
                },
            )
        }

        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                onStockClick = { symbol ->
                    navController.navigate(Screen.StockDetail.createRoute(symbol))
                },
            )
        }

        composable(Screen.Portfolio.route) {
            PortfolioScreen()
        }

        // ─── Detail Screens ───────────────────────────────────────────────────
        composable(
            route = Screen.StockDetail.route,
            arguments = listOf(
                navArgument(Screen.StockDetail.ARG_SYMBOL) { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString(Screen.StockDetail.ARG_SYMBOL) ?: ""
            StockDetailScreen(
                symbol = symbol,
                onBack = { navController.popBackStack() },
                onOptionChainClick = {
                    navController.navigate(Screen.OptionChain.createRoute(symbol))
                },
            )
        }

        composable(
            route = Screen.OptionChain.route,
            arguments = listOf(
                navArgument(Screen.OptionChain.ARG_SYMBOL) { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString(Screen.OptionChain.ARG_SYMBOL) ?: ""
            OptionChainScreen(
                symbol = symbol,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.MarketIndex.route,
            arguments = listOf(
                navArgument(Screen.MarketIndex.ARG_INDEX_NAME) { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val indexName = backStackEntry.arguments?.getString(Screen.MarketIndex.ARG_INDEX_NAME) ?: ""
            MarketIndexScreen(
                indexName = indexName,
                onStockClick = { symbol ->
                    navController.navigate(Screen.StockDetail.createRoute(symbol))
                },
                onBack = { navController.popBackStack() },
            )
        }

        // ─── Settings ─────────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
