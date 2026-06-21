package com.example.webviewbrowser.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.webviewbrowser.browser.BrowserIntent
import com.example.webviewbrowser.browser.BrowserViewModel
import com.example.webviewbrowser.browser.ui.BrowserScreen
import com.example.webviewbrowser.bookmarks.ui.BookmarksScreen
import com.example.webviewbrowser.downloads.ui.DownloadsScreen
import com.example.webviewbrowser.history.ui.HistoryScreen
import com.example.webviewbrowser.settings.ui.SettingsScreen

/**
 * アプリのナビゲーション。
 *
 * BrowserViewModel は Activity スコープで共有し、Bookmarks/History から
 * URL を開くときに同一インスタンスへ Intent を送る。
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    requestedDestination: String? = null,
    onDestinationConsumed: () -> Unit = {},
) {
    val activity = LocalContext.current as ComponentActivity
    val browserViewModel: BrowserViewModel = hiltViewModel(activity)

    fun openUrlInBrowser(url: String) {
        browserViewModel.onIntent(BrowserIntent.SubmitAddress(url))
        navController.navigate(AppRoutes.BROWSER) {
            popUpTo(AppRoutes.BROWSER) { inclusive = true }
        }
    }

    LaunchedEffect(requestedDestination) {
        requestedDestination?.let { route ->
            navController.navigate(route) { launchSingleTop = true }
            onDestinationConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.BROWSER,
        modifier = modifier,
    ) {
        composable(AppRoutes.BROWSER) {
            BrowserScreen(
                onOpenBookmarks = { navController.navigate(AppRoutes.BOOKMARKS) },
                onOpenHistory = { navController.navigate(AppRoutes.HISTORY) },
                onOpenDownloads = { navController.navigate(AppRoutes.DOWNLOADS) },
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                viewModel = browserViewModel,
            )
        }
        composable(AppRoutes.BOOKMARKS) {
            BookmarksScreen(onOpenUrl = ::openUrlInBrowser, onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.HISTORY) {
            HistoryScreen(onOpenUrl = ::openUrlInBrowser, onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.DOWNLOADS) {
            DownloadsScreen(onBack = { navController.popBackStack() })
        }
    }
}
