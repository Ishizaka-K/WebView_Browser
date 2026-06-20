package com.example.webviewbrowser.browser

import com.example.webviewbrowser.data.prefs.model.AppSettings

/**
 * ブラウザ状態の純粋な遷移ロジック。
 *
 * WebView イベント由来の更新と、タブ追加/削除/切替の純粋ヘルパを提供する。
 * 副作用（loadUrl, 履歴記録, セッション保存）は ViewModel が担当する。
 */
object BrowserReducer {

    /** ホーム表示用の URL sentinel。 */
    const val HOME = AppSettings.HOME_SENTINEL

    fun reduce(state: BrowserState, intent: BrowserIntent): BrowserState = when (intent) {
        is BrowserIntent.AddressInputChanged -> state.copy(addressInput = intent.text)

        is BrowserIntent.PageStarted -> state.updateTab(intent.tabId) {
            it.copy(url = intent.url, progress = 0, isLoading = true, isError = false, isHome = false)
        }

        is BrowserIntent.ProgressChanged -> state.updateTab(intent.tabId) {
            it.copy(progress = intent.progress, isLoading = intent.progress < 100)
        }

        is BrowserIntent.TitleChanged -> state.updateTab(intent.tabId) {
            it.copy(title = intent.title)
        }

        is BrowserIntent.PageFinished -> {
            val newState = state.updateTab(intent.tabId) {
                it.copy(
                    url = intent.url,
                    progress = 100,
                    isLoading = false,
                    canGoBack = intent.canGoBack,
                    canGoForward = intent.canGoForward,
                )
            }
            // アクティブタブなら address 表示を最終 URL に同期する。
            if (intent.tabId == state.activeTabId) {
                newState.copy(addressInput = intent.url)
            } else {
                newState
            }
        }

        is BrowserIntent.ReceivedError -> if (intent.isForMainFrame) {
            state.updateTab(intent.tabId) { it.copy(isError = true, isLoading = false) }
        } else {
            state
        }

        is BrowserIntent.SwitchTab -> {
            val target = state.tabs.firstOrNull { it.id == intent.tabId } ?: return state
            state.copy(activeTabId = intent.tabId, addressInput = target.displayAddress())
        }

        is BrowserIntent.MoveTab -> withMovedTab(state, intent.fromIndex, intent.toIndex)

        is BrowserIntent.ToggleFind -> state.copy(
            find = if (intent.active) FindState(active = true) else FindState(active = false),
        )

        is BrowserIntent.FindQueryChanged -> state.copy(find = state.find.copy(query = intent.query))

        is BrowserIntent.FindResult -> if (intent.tabId == state.activeTabId) {
            state.copy(find = state.find.copy(matchCount = intent.matchCount, activeIndex = intent.activeIndex))
        } else {
            state
        }

        // 以下は ViewModel 側で副作用とともに処理する。
        else -> state
    }

    /** タブを追加する。activate=true ならアクティブにする。 */
    fun withNewTab(state: BrowserState, tab: TabUiState, activate: Boolean): BrowserState {
        val tabs = state.tabs + tab
        return if (activate) {
            state.copy(tabs = tabs, activeTabId = tab.id, addressInput = tab.displayAddress())
        } else {
            state.copy(tabs = tabs)
        }
    }

    /**
     * タブを閉じる。アクティブを閉じたら隣接をアクティブにする。
     * 空になる場合は [emptyTabFactory] が生成するホームタブを 1 つ残す。
     */
    fun withClosedTab(
        state: BrowserState,
        tabId: String,
        emptyTabFactory: () -> TabUiState,
    ): BrowserState {
        val index = state.tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return state
        val remaining = state.tabs.toMutableList().apply { removeAt(index) }

        if (remaining.isEmpty()) {
            val home = emptyTabFactory()
            return state.copy(tabs = listOf(home), activeTabId = home.id, addressInput = home.displayAddress())
        }

        val newActiveId = if (state.activeTabId == tabId) {
            remaining[index.coerceAtMost(remaining.lastIndex)].id
        } else {
            state.activeTabId
        }
        val activeAddress = remaining.firstOrNull { it.id == newActiveId }?.displayAddress() ?: ""
        return state.copy(
            tabs = remaining,
            activeTabId = newActiveId,
            addressInput = if (state.activeTabId == tabId) activeAddress else state.addressInput,
        )
    }

    /** タブ一覧でのドラッグ並べ替えを状態に反映する。 */
    fun withMovedTab(state: BrowserState, fromIndex: Int, toIndex: Int): BrowserState {
        if (fromIndex == toIndex) return state
        if (fromIndex !in state.tabs.indices || toIndex !in state.tabs.indices) return state
        val moved = state.tabs.toMutableList()
        val tab = moved.removeAt(fromIndex)
        moved.add(toIndex, tab)
        return state.copy(tabs = moved)
    }

    /** アクティブタブをホームに戻す。 */
    fun withActiveHome(state: BrowserState): BrowserState =
        state.updateTab(state.activeTabId) {
            it.copy(url = HOME, title = "Home", isHome = true, isLoading = false, progress = 0, isError = false)
        }.copy(addressInput = "")

    private fun BrowserState.updateTab(tabId: String?, transform: (TabUiState) -> TabUiState): BrowserState {
        if (tabId == null) return this
        return copy(tabs = tabs.map { if (it.id == tabId) transform(it) else it })
    }
}

/** ホームタブはアドレス欄を空表示にする。 */
fun TabUiState.displayAddress(): String = if (isHome) "" else url
