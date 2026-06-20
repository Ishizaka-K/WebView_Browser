package com.example.webviewbrowser.browser

/** 検索エンジン定義。id とクエリテンプレート(%s)。 */
data class SearchEngine(val id: String, val label: String, val template: String)

object SearchEngines {
    val ALL = listOf(
        SearchEngine("google", "Google", "https://www.google.com/search?q=%s"),
        SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
        SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    )

    private val DEFAULT = ALL.first()

    fun templateFor(id: String): String =
        ALL.firstOrNull { it.id == id }?.template ?: DEFAULT.template
}
