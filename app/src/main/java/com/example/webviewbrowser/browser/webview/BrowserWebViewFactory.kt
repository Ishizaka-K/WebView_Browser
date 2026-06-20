package com.example.webviewbrowser.browser.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * WebView 生成と既定設定を担う。
 *
 * 設定の動的反映（JS/文字サイズ等）は Task9 の WebSettingsApplier が担当する。
 * ここでは安全側の既定値のみ適用する。
 */
object BrowserWebViewFactory {

    @SuppressLint("SetJavaScriptEnabled")
    fun create(
        context: Context,
        tabId: String,
        generationProvider: () -> Int,
        emit: (WebViewEvent) -> Unit,
        onIcon: (android.graphics.Bitmap) -> Unit,
    ): WebView {
        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // セキュリティ既定: ローカルファイルアクセスを無効化。
            allowFileAccess = false
            allowContentAccess = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // 安全側既定: SafeBrowsing 有効。
            safeBrowsingEnabled = true
        }
        // third-party cookie はブラウザとして既定許可。Task9 の設定で連動予定。
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = BrowserWebViewClient(tabId, generationProvider, emit)
        webView.webChromeClient = BrowserWebChromeClient(tabId, generationProvider, emit, onIcon)
        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            emit(
                WebViewEvent.FindResult(
                    tabId = tabId,
                    generation = generationProvider(),
                    activeMatchIndex = activeMatchOrdinal,
                    matchCount = numberOfMatches,
                    isDoneCounting = isDoneCounting,
                ),
            )
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val cookie = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
            emit(
                WebViewEvent.DownloadRequested(
                    tabId = tabId,
                    generation = generationProvider(),
                    event = WebViewDownloadEvent(
                        url = url,
                        mimeType = mimeType,
                        contentLength = contentLength,
                        contentDisposition = contentDisposition,
                        userAgent = userAgent,
                        cookieHeader = cookie,
                        referer = webView.url,
                        sourcePageUrl = webView.url,
                    ),
                ),
            )
        }
        return webView
    }
}
