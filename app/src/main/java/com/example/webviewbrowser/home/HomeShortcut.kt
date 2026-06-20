package com.example.webviewbrowser.home

/** ホームのショートカット。 */
data class HomeShortcut(val title: String, val url: String) {
    /** Google のファビコンサービス URL。 */
    val faviconUrl: String
        get() = "https://www.google.com/s2/favicons?sz=128&domain_url=$url"

    companion object {
        /** 初期表示する人気サイト。 */
        val DEFAULTS = listOf(
            HomeShortcut("Google", "https://www.google.com"),
            HomeShortcut("YouTube", "https://www.youtube.com"),
            HomeShortcut("X", "https://x.com"),
            HomeShortcut("Gmail", "https://mail.google.com"),
            HomeShortcut("Maps", "https://maps.google.com"),
            HomeShortcut("Amazon", "https://www.amazon.co.jp"),
            HomeShortcut("Wikipedia", "https://ja.wikipedia.org"),
            HomeShortcut("GitHub", "https://github.com"),
        )
    }
}
