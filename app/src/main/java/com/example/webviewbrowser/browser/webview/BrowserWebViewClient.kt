package com.example.webviewbrowser.browser.webview

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * ページ遷移・エラー・レンダラ消失を [WebViewEvent] として通知する WebViewClient。
 *
 * tabId/generation を付与するため、閉じたタブや再生成前のイベントを利用側が破棄できる。
 */
class BrowserWebViewClient(
    private val tabId: String,
    private val generationProvider: () -> Int,
    private val emit: (WebViewEvent) -> Unit,
) : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        emit(WebViewEvent.PageStarted(tabId, generationProvider(), url.orEmpty()))
    }

    override fun onPageFinished(view: WebView, url: String?) {
        emit(
            WebViewEvent.PageFinished(
                tabId = tabId,
                generation = generationProvider(),
                url = url ?: view.url.orEmpty(),
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
                isError = false,
            ),
        )
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        val isMainFrame = request?.isForMainFrame == true
        emit(
            WebViewEvent.ReceivedError(
                tabId = tabId,
                generation = generationProvider(),
                url = request?.url?.toString(),
                isForMainFrame = isMainFrame,
                description = error?.description?.toString(),
            ),
        )
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        // main frame の HTTP エラー(4xx/5xx)はエラー扱いにして履歴記録対象から除く。
        if (request?.isForMainFrame == true) {
            emit(
                WebViewEvent.ReceivedError(
                    tabId = tabId,
                    generation = generationProvider(),
                    url = request.url?.toString(),
                    isForMainFrame = true,
                    description = "HTTP ${errorResponse?.statusCode}",
                ),
            )
        }
    }

    override fun onRenderProcessGone(
        view: WebView,
        detail: android.webkit.RenderProcessGoneDetail?,
    ): Boolean {
        emit(WebViewEvent.RenderProcessGone(tabId, generationProvider()))
        // true を返してプロセスを引き取り、アプリ全体のクラッシュを防ぐ。
        // 利用側がタブを再生成する。
        return true
    }
}
