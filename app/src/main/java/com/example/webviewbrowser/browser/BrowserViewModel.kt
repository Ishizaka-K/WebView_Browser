package com.example.webviewbrowser.browser

import androidx.lifecycle.viewModelScope
import com.example.webviewbrowser.bookmarks.domain.BookmarkUseCase
import com.example.webviewbrowser.browser.webview.TabManager
import com.example.webviewbrowser.browser.webview.WebViewEvent
import com.example.webviewbrowser.core.mvi.MviViewModel
import com.example.webviewbrowser.core.port.HistoryRecorder
import com.example.webviewbrowser.core.util.NormalizedTarget
import com.example.webviewbrowser.core.util.UrlNormalizer
import com.example.webviewbrowser.data.prefs.SettingsDataStore
import com.example.webviewbrowser.data.prefs.model.AppSettings
import com.example.webviewbrowser.data.session.SessionRepository
import com.example.webviewbrowser.data.session.SessionTab
import com.example.webviewbrowser.downloads.DownloadRequestMapper
import com.example.webviewbrowser.downloads.DownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabManager: TabManager,
    private val sessionRepository: SessionRepository,
    private val historyRecorder: HistoryRecorder,
    private val downloadUseCase: DownloadUseCase,
    private val bookmarkUseCase: BookmarkUseCase,
    settingsDataStore: SettingsDataStore,
) : MviViewModel<BrowserState, BrowserIntent, BrowserEffect>(BrowserState()) {

    private val settings = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings.DEFAULT)

    init {
        observeWebViewEvents()
        restoreSession()
    }

    override fun reduce(state: BrowserState, intent: BrowserIntent): BrowserState =
        BrowserReducer.reduce(state, intent)

    /** UI 層が AndroidView にアタッチするための WebView を取得する（必要なら生成）。 */
    fun acquireWebView(tabId: String) = tabManager.getOrCreate(tabId)

    /** タブ一覧を開く前に、アクティブタブの表示をサムネイルとして取り込む。 */
    fun captureActiveThumbnail() {
        currentState.activeTabId?.let { tabManager.captureThumbnail(it) }
    }

    fun tabThumbnail(tabId: String) = tabManager.thumbnail(tabId)
    fun tabFavicon(tabId: String) = tabManager.favicon(tabId)

    /** PixelCopy で取得したサムネイルを保存する。 */
    fun setThumbnail(tabId: String, bitmap: android.graphics.Bitmap) = tabManager.putThumbnail(tabId, bitmap)

    override fun onIntent(intent: BrowserIntent) {
        when (intent) {
            is BrowserIntent.AddressInputChanged -> dispatch(intent)
            is BrowserIntent.SubmitAddress -> submitAddress(intent.text)
            BrowserIntent.GoBack -> currentState.activeTabId?.let { tabManager.goBack(it) }
            BrowserIntent.GoForward -> currentState.activeTabId?.let { tabManager.goForward(it) }
            BrowserIntent.Reload -> currentState.activeTabId?.let { tabManager.reload(it) }
            BrowserIntent.StopLoading -> {
                currentState.activeTabId?.let { tabManager.stopLoading(it) }
                setState { it.markActive { tab -> tab.copy(isLoading = false) } }
            }
            BrowserIntent.GoHome -> goHome()
            is BrowserIntent.NewTab -> openNewTab(intent.activate)
            is BrowserIntent.CloseTab -> closeTab(intent.tabId)
            is BrowserIntent.MoveTab -> moveTab(intent.fromIndex, intent.toIndex)
            is BrowserIntent.SwitchTab -> {
                dispatch(intent)
                ensureLoaded(intent.tabId)
                persistSession()
            }
            BrowserIntent.ShareCurrent -> currentState.activeTab?.let {
                if (!it.isHome) emitEffect(BrowserEffect.ShareUrl(it.url, it.title))
            }
            BrowserIntent.ToggleBookmarkCurrent -> toggleBookmark()
            is BrowserIntent.ToggleFind -> {
                dispatch(intent)
                if (!intent.active) currentState.activeTabId?.let { tabManager.clearFind(it) }
            }
            is BrowserIntent.FindQueryChanged -> {
                dispatch(intent)
                currentState.activeTabId?.let { tabManager.findInPage(it, intent.query) }
            }
            is BrowserIntent.FindNext -> currentState.activeTabId?.let { tabManager.findNext(it, intent.forward) }
            is BrowserIntent.FindResult -> dispatch(intent)

            is BrowserIntent.PageStarted -> dispatch(intent)
            is BrowserIntent.ProgressChanged -> dispatch(intent)
            is BrowserIntent.TitleChanged -> dispatch(intent)
            is BrowserIntent.PageFinished -> {
                dispatch(intent)
                recordHistory(intent)
                persistSession()
            }
            is BrowserIntent.ReceivedError -> dispatch(intent)
            is BrowserIntent.RenderProcessGone -> recreateTab(intent.tabId)
            is BrowserIntent.DownloadRequested -> {
                val request = DownloadRequestMapper.map(intent.event)
                viewModelScope.launch {
                    downloadUseCase.enqueue(request)
                    emitEffect(BrowserEffect.ShowMessage("ダウンロードを開始しました: ${request.fileName}"))
                }
            }
        }
    }

    fun moveTab(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        dispatch(BrowserIntent.MoveTab(fromIndex, toIndex))
        persistSession()
    }

    private fun submitAddress(text: String) {
        val tabId = currentState.activeTabId ?: return
        val template = SearchEngines.templateFor(settings.value.searchEngineId)
        val url = when (val target = UrlNormalizer.normalize(text, template)) {
            is NormalizedTarget.Url -> target.url
            is NormalizedTarget.Search -> target.url
        }
        setState {
            it.markActive { tab -> tab.copy(isHome = false, isLoading = true, url = url, isError = false) }
        }
        tabManager.loadUrl(tabId, url)
    }

    private fun goHome() {
        val tabId = currentState.activeTabId ?: return
        val homeUrl = settings.value.homeUrl
        if (homeUrl.isNotBlank() && homeUrl != BrowserReducer.HOME) {
            // ホーム URL が設定されていれば通常ロードする。
            submitAddress(homeUrl)
            return
        }
        // 純粋なスタートページ。WebView を破棄して遅延 callback を断つ（generation 失効）。
        tabManager.closeTab(tabId)
        setState { BrowserReducer.withActiveHome(it) }
        persistSession()
    }

    private fun toggleBookmark() {
        val tab = currentState.activeTab ?: return
        if (tab.isHome) return
        viewModelScope.launch {
            bookmarkUseCase.toggle(tab.title.ifBlank { tab.url }, tab.url)
            emitEffect(BrowserEffect.ShowMessage("ブックマークを更新しました"))
        }
    }

    private fun openNewTab(activate: Boolean) {
        val tab = newHomeTab()
        setState { BrowserReducer.withNewTab(it, tab, activate) }
        persistSession()
    }

    private fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
        setState { BrowserReducer.withClosedTab(it, tabId) { newHomeTab() } }
        persistSession()
    }

    private fun recreateTab(tabId: String) {
        val tab = currentState.tabs.firstOrNull { it.id == tabId } ?: return
        tabManager.recreate(tabId)
        if (!tab.isHome) {
            tabManager.loadUrl(tabId, tab.url)
        }
        emitEffect(BrowserEffect.ShowMessage("ページを再読み込みしました"))
    }

    /** 非ホームタブで WebView が未ロードなら現在 URL を読み込む。 */
    private fun ensureLoaded(tabId: String) {
        val tab = currentState.tabs.firstOrNull { it.id == tabId } ?: return
        if (!tab.isHome && tabManager.get(tabId)?.url == null) {
            tabManager.loadUrl(tabId, tab.url)
        }
    }

    private fun recordHistory(intent: BrowserIntent.PageFinished) {
        val tab = currentState.tabs.firstOrNull { it.id == intent.tabId } ?: return
        val url = intent.url
        if (tab.isError) return
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        viewModelScope.launch {
            historyRecorder.record(url, tab.title.ifBlank { url })
        }
    }

    private fun observeWebViewEvents() {
        viewModelScope.launch {
            tabManager.events.collect { event ->
                // 存在しないタブ（閉じた後）のイベントは無視する。
                if (currentState.tabs.none { it.id == event.tabId }) return@collect
                // 再生成前（stale）の WebView からのイベントは generation で破棄する。
                if (event.generation != tabManager.currentGeneration(event.tabId)) return@collect
                event.toIntent()?.let { onIntent(it) }
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val restored = sessionRepository.restore()
            val tabs = if (restored.isEmpty()) {
                listOf(newHomeTab())
            } else {
                restored.map { it.toTabUiState() }
            }
            val activeId = tabs.firstOrNull { restoredActive(restored, it.id) }?.id ?: tabs.first().id
            setState {
                BrowserState(
                    tabs = tabs,
                    activeTabId = activeId,
                    addressInput = tabs.firstOrNull { it.id == activeId }?.displayAddress() ?: "",
                )
            }
            // 復元したアクティブな Web タブを読み込む。
            currentState.activeTab?.let { if (!it.isHome) tabManager.loadUrl(it.id, it.url) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Activity が真に終了したとき（構成変更ではない）に WebView を破棄する。
        tabManager.destroyAll()
    }

    private fun restoredActive(restored: List<SessionTab>, id: String): Boolean =
        restored.firstOrNull { it.id == id }?.isActive == true

    private fun persistSession() {
        val snapshot = currentState
        viewModelScope.launch {
            sessionRepository.save(
                snapshot.tabs.map { tab ->
                    SessionTab(
                        id = tab.id,
                        url = if (tab.isHome) BrowserReducer.HOME else tab.url,
                        title = tab.title,
                        isActive = tab.id == snapshot.activeTabId,
                        isHome = tab.isHome,
                    )
                },
            )
        }
    }

    private fun newHomeTab(): TabUiState =
        TabUiState(id = UUID.randomUUID().toString(), url = BrowserReducer.HOME, title = "Home", isHome = true)

    private fun SessionTab.toTabUiState(): TabUiState =
        TabUiState(id = id, url = url, title = title.ifBlank { if (isHome) "Home" else url }, isHome = isHome)

    private fun BrowserState.markActive(transform: (TabUiState) -> TabUiState): BrowserState =
        copy(tabs = tabs.map { if (it.id == activeTabId) transform(it) else it })

    private fun WebViewEvent.toIntent(): BrowserIntent? = when (this) {
        is WebViewEvent.PageStarted -> BrowserIntent.PageStarted(tabId, url)
        is WebViewEvent.ProgressChanged -> BrowserIntent.ProgressChanged(tabId, progress)
        is WebViewEvent.TitleChanged -> BrowserIntent.TitleChanged(tabId, title)
        is WebViewEvent.PageFinished -> BrowserIntent.PageFinished(tabId, url, canGoBack, canGoForward)
        is WebViewEvent.ReceivedError -> BrowserIntent.ReceivedError(tabId, isForMainFrame)
        is WebViewEvent.RenderProcessGone -> BrowserIntent.RenderProcessGone(tabId)
        is WebViewEvent.DownloadRequested -> BrowserIntent.DownloadRequested(event)
        is WebViewEvent.FindResult -> BrowserIntent.FindResult(tabId, activeMatchIndex, matchCount)
    }
}
