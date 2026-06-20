package com.example.webviewbrowser.browser.webview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * タブごとの WebView を保持・管理する。
 *
 * 規約:
 * - すべての WebView 操作は [MainThread] で行う。
 * - Activity context leak を避けるため Application context で WebView を生成する。
 * - 実体保持上限は [MAX_LIVE_WEBVIEWS]。超過時は LRU で非アクティブを破棄する。
 * - WebView callback は tabId/generation 付きで [events] に流す。
 */
@ActivityRetainedScoped
class TabManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private class Holder(
        val webView: WebView,
        var generation: Int,
        var lastAccess: Long,
    )

    private val holders = mutableMapOf<String, Holder>()
    private val thumbnails = mutableMapOf<String, Bitmap>()
    private val favicons = mutableMapOf<String, Bitmap>()

    private val _events = MutableSharedFlow<WebViewEvent>(extraBufferCapacity = 64)
    val events: Flow<WebViewEvent> = _events.asSharedFlow()

    /** 現在実体を持つタブ ID 一覧。 */
    @MainThread
    fun liveTabIds(): Set<String> = holders.keys.toSet()

    /**
     * タブの WebView を取得する。なければ生成する。
     * 生成・取得時に LRU の最終アクセス時刻を更新し、必要なら eviction する。
     */
    @MainThread
    fun getOrCreate(tabId: String): WebView {
        holders[tabId]?.let {
            it.lastAccess = now()
            return it.webView
        }
        val holder = Holder(
            webView = createWebView(tabId, generation = 0),
            generation = 0,
            lastAccess = now(),
        )
        holders[tabId] = holder
        evictIfNeeded(keep = tabId)
        return holder.webView
    }

    @MainThread
    fun get(tabId: String): WebView? = holders[tabId]?.webView

    /** 現在の generation。stale callback 判定に使う。 */
    @MainThread
    fun currentGeneration(tabId: String): Int? = holders[tabId]?.generation

    /** タブのサムネイル（タブ一覧用）。 */
    fun thumbnail(tabId: String): Bitmap? = thumbnails[tabId]

    /** PixelCopy 等で取得したサムネイルを保存する。 */
    fun putThumbnail(tabId: String, bitmap: Bitmap) {
        thumbnails[tabId] = bitmap
    }

    /** タブのファビコン。 */
    fun favicon(tabId: String): Bitmap? = favicons[tabId]

    /** 現在の WebView 表示をサムネイルとして取り込む。 */
    @MainThread
    fun captureThumbnail(tabId: String) {
        val webView = holders[tabId]?.webView ?: return
        if (webView.width <= 0 || webView.height <= 0) return
        val scale = 0.4f
        val w = (webView.width * scale).toInt().coerceAtLeast(1)
        val h = (webView.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        webView.draw(canvas)
        thumbnails[tabId] = bitmap
    }

    @MainThread
    fun loadUrl(tabId: String, url: String) {
        getOrCreate(tabId).loadUrl(url)
    }

    @MainThread
    fun goBack(tabId: String) {
        holders[tabId]?.webView?.takeIf { it.canGoBack() }?.goBack()
    }

    @MainThread
    fun goForward(tabId: String) {
        holders[tabId]?.webView?.takeIf { it.canGoForward() }?.goForward()
    }

    @MainThread
    fun reload(tabId: String) {
        holders[tabId]?.webView?.reload()
    }

    @MainThread
    fun stopLoading(tabId: String) {
        holders[tabId]?.webView?.stopLoading()
    }

    @MainThread
    fun canGoBack(tabId: String): Boolean = holders[tabId]?.webView?.canGoBack() == true

    @MainThread
    fun findInPage(tabId: String, query: String) {
        holders[tabId]?.webView?.findAllAsync(query)
    }

    @MainThread
    fun findNext(tabId: String, forward: Boolean) {
        holders[tabId]?.webView?.findNext(forward)
    }

    @MainThread
    fun clearFind(tabId: String) {
        holders[tabId]?.webView?.clearMatches()
    }

    /** タブを閉じて WebView を破棄する。 */
    @MainThread
    fun closeTab(tabId: String) {
        holders.remove(tabId)?.let { destroy(it.webView) }
        thumbnails.remove(tabId)
        favicons.remove(tabId)
    }

    /** すべての WebView を破棄する（Activity 破棄時など）。 */
    @MainThread
    fun destroyAll() {
        holders.values.forEach { destroy(it.webView) }
        holders.clear()
    }

    /**
     * レンダラ消失時にタブを再生成する。generation を進めて stale callback を無効化する。
     */
    @MainThread
    fun recreate(tabId: String): WebView {
        val old = holders[tabId]
        val newGeneration = (old?.generation ?: 0) + 1
        old?.let { destroy(it.webView) }
        val holder = Holder(
            webView = createWebView(tabId, newGeneration),
            generation = newGeneration,
            lastAccess = now(),
        )
        holders[tabId] = holder
        return holder.webView
    }

    private fun createWebView(tabId: String, generation: Int): WebView =
        BrowserWebViewFactory.create(
            context = context,
            tabId = tabId,
            // 生成時の generation を固定で閉じ込める。再生成後に旧 WebView の
            // callback が来ても、この値は古いままなので利用側が stale と判定できる。
            generationProvider = { generation },
            emit = { event -> _events.tryEmit(event) },
            onIcon = { bitmap -> favicons[tabId] = bitmap },
        )

    private fun evictIfNeeded(keep: String) {
        if (holders.size <= MAX_LIVE_WEBVIEWS) return
        // keep 以外で最も古いものを破棄する。
        val victim = holders.entries
            .filter { it.key != keep }
            .minByOrNull { it.value.lastAccess }
            ?: return
        holders.remove(victim.key)
        destroy(victim.value.webView)
    }

    private fun destroy(webView: WebView) {
        webView.stopLoading()
        webView.webViewClient = NoOpWebViewClient
        webView.webChromeClient = null
        webView.setFindListener(null)
        webView.setDownloadListener(null)
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.removeAllViews()
        webView.destroy()
    }

    private fun now(): Long = System.currentTimeMillis()

    private object NoOpWebViewClient : android.webkit.WebViewClient()

    companion object {
        const val MAX_LIVE_WEBVIEWS = 4
    }
}
