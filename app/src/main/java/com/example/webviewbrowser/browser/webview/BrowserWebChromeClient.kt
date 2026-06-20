package com.example.webviewbrowser.browser.webview

import android.graphics.Bitmap
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * 進捗・タイトル・ファビコン・権限要求を扱う WebChromeClient。
 *
 * v1 では権限要求（カメラ/マイク等）と位置情報は deny 既定とする。
 */
class BrowserWebChromeClient(
    private val tabId: String,
    private val generationProvider: () -> Int,
    private val emit: (WebViewEvent) -> Unit,
    private val onIcon: (Bitmap) -> Unit,
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        emit(WebViewEvent.ProgressChanged(tabId, generationProvider(), newProgress))
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        if (title != null) {
            emit(WebViewEvent.TitleChanged(tabId, generationProvider(), title))
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        if (icon != null) onIcon(icon)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        // v1: サイト権限はグローバル deny 既定。
        request?.deny()
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        // v1: 位置情報は deny 既定（保存しない）。
        callback?.invoke(origin, false, false)
    }
}
