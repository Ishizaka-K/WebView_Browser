package com.example.webviewbrowser.data.prefs.model

/** Cookie 受け入れポリシー。 */
enum class CookiePolicy { ALL, NONE }

/** テーマモード。 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * アプリ設定のスナップショット。
 */
data class AppSettings(
    val searchEngineId: String,
    val homeUrl: String,
    val javascriptEnabled: Boolean,
    val blockPopups: Boolean,
    val cookiePolicy: CookiePolicy,
    val doNotTrack: Boolean,
    val themeMode: ThemeMode,
    val textScaling: Int,
    val maxParallelDownloads: Int,
) {
    companion object {
        /** ホームの内部 sentinel。WebView には渡さない。 */
        const val HOME_SENTINEL = "home://start"

        val DEFAULT = AppSettings(
            searchEngineId = "google",
            homeUrl = HOME_SENTINEL,
            javascriptEnabled = true,
            blockPopups = true,
            cookiePolicy = CookiePolicy.ALL,
            doNotTrack = false,
            themeMode = ThemeMode.SYSTEM,
            textScaling = 100,
            maxParallelDownloads = 3,
        )
    }
}
