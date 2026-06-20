package com.example.webviewbrowser.browser

import com.example.webviewbrowser.browser.webview.WebViewDownloadEvent
import com.example.webviewbrowser.core.mvi.MviEffect
import com.example.webviewbrowser.core.mvi.MviIntent
import com.example.webviewbrowser.core.mvi.MviState

/** 1 タブの UI 状態。 */
data class TabUiState(
    val id: String,
    val url: String,
    val title: String,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isError: Boolean = false,
    val isHome: Boolean = true,
)

/** ページ内検索の状態。 */
data class FindState(
    val active: Boolean = false,
    val query: String = "",
    val matchCount: Int = 0,
    val activeIndex: Int = 0,
)

/** ブラウザ画面の状態。 */
data class BrowserState(
    val tabs: List<TabUiState> = emptyList(),
    val activeTabId: String? = null,
    val addressInput: String = "",
    val find: FindState = FindState(),
) : MviState {
    val activeTab: TabUiState? get() = tabs.firstOrNull { it.id == activeTabId }
    val tabCount: Int get() = tabs.size
}

/** ブラウザ画面の Intent。ユーザー操作と WebView コールバック由来を統一的に表す。 */
sealed interface BrowserIntent : MviIntent {
    // ユーザー操作
    data class AddressInputChanged(val text: String) : BrowserIntent
    data class SubmitAddress(val text: String) : BrowserIntent
    data object GoBack : BrowserIntent
    data object GoForward : BrowserIntent
    data object Reload : BrowserIntent
    data object StopLoading : BrowserIntent
    data object GoHome : BrowserIntent
    data class NewTab(val activate: Boolean = true) : BrowserIntent
    data class CloseTab(val tabId: String) : BrowserIntent
    data class SwitchTab(val tabId: String) : BrowserIntent
    data class MoveTab(val fromIndex: Int, val toIndex: Int) : BrowserIntent
    data object ShareCurrent : BrowserIntent
    data object ToggleBookmarkCurrent : BrowserIntent
    data class ToggleFind(val active: Boolean) : BrowserIntent
    data class FindQueryChanged(val query: String) : BrowserIntent
    data class FindNext(val forward: Boolean) : BrowserIntent
    data class FindResult(val tabId: String, val activeIndex: Int, val matchCount: Int) : BrowserIntent

    // WebView コールバック由来
    data class PageStarted(val tabId: String, val url: String) : BrowserIntent
    data class ProgressChanged(val tabId: String, val progress: Int) : BrowserIntent
    data class TitleChanged(val tabId: String, val title: String) : BrowserIntent
    data class PageFinished(
        val tabId: String,
        val url: String,
        val canGoBack: Boolean,
        val canGoForward: Boolean,
    ) : BrowserIntent
    data class ReceivedError(val tabId: String, val isForMainFrame: Boolean) : BrowserIntent
    data class RenderProcessGone(val tabId: String) : BrowserIntent
    data class DownloadRequested(val event: WebViewDownloadEvent) : BrowserIntent
}

/** 1 回限りの副作用。 */
sealed interface BrowserEffect : MviEffect {
    data class RequestDownload(val event: WebViewDownloadEvent) : BrowserEffect
    data class ShareUrl(val url: String, val title: String?) : BrowserEffect
    data class ShowMessage(val message: String) : BrowserEffect
}
