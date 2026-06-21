package com.example.webviewbrowser

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.webviewbrowser.core.ui.theme.WebViewBrowserTheme
import com.example.webviewbrowser.data.prefs.model.ThemeMode
import com.example.webviewbrowser.navigation.AppNavHost
import com.example.webviewbrowser.navigation.AppRoutes
import com.example.webviewbrowser.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 単一 Activity。Compose の NavHost をホストする。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var requestedDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedDestination = intent.requestedDestination()
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (appSettings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            WebViewBrowserTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    requestedDestination = requestedDestination,
                    onDestinationConsumed = { requestedDestination = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedDestination = intent.requestedDestination()
    }

    private fun Intent.requestedDestination(): String? =
        getStringExtra(EXTRA_DESTINATION)?.takeIf { it == AppRoutes.DOWNLOADS }

    companion object {
        const val EXTRA_DESTINATION = "destination"
    }
}
