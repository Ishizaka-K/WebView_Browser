package com.example.webviewbrowser.browser.webview

/**
 * WebView 由来のダウンロード要求 DTO。
 *
 * browser/webview が所有し、downloads ドメインの DownloadRequest には依存しない。
 * WebViewDownloadEvent -> DownloadRequest の変換は統合層(Task11)が行う。
 */
data class WebViewDownloadEvent(
    val url: String,
    val mimeType: String?,
    val contentLength: Long,
    val contentDisposition: String?,
    val userAgent: String?,
    val cookieHeader: String?,
    val referer: String?,
    val sourcePageUrl: String?,
)

/**
 * WebView コールバックを表すイベント。
 *
 * すべて [tabId] と [generation] を持つ。閉じたタブや再生成前のイベントは
 * 利用側が generation で識別して破棄できる。
 */
sealed interface WebViewEvent {
    val tabId: String
    val generation: Int

    data class PageStarted(
        override val tabId: String,
        override val generation: Int,
        val url: String,
    ) : WebViewEvent

    data class ProgressChanged(
        override val tabId: String,
        override val generation: Int,
        val progress: Int,
    ) : WebViewEvent

    data class TitleChanged(
        override val tabId: String,
        override val generation: Int,
        val title: String,
    ) : WebViewEvent

    data class PageFinished(
        override val tabId: String,
        override val generation: Int,
        val url: String,
        val canGoBack: Boolean,
        val canGoForward: Boolean,
        val isError: Boolean,
    ) : WebViewEvent

    data class ReceivedError(
        override val tabId: String,
        override val generation: Int,
        val url: String?,
        val isForMainFrame: Boolean,
        val description: String?,
    ) : WebViewEvent

    data class RenderProcessGone(
        override val tabId: String,
        override val generation: Int,
    ) : WebViewEvent

    data class FindResult(
        override val tabId: String,
        override val generation: Int,
        val activeMatchIndex: Int,
        val matchCount: Int,
        val isDoneCounting: Boolean,
    ) : WebViewEvent

    data class DownloadRequested(
        override val tabId: String,
        override val generation: Int,
        val event: WebViewDownloadEvent,
    ) : WebViewEvent

    /** WebView では処理できない外部アプリ向け URL。 */
    data class ExternalNavigationRequested(
        override val tabId: String,
        override val generation: Int,
        val url: String,
    ) : WebViewEvent
}
