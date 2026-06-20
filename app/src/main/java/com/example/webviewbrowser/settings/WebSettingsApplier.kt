package com.example.webviewbrowser.settings

import android.webkit.CookieManager
import android.webkit.WebView
import com.example.webviewbrowser.data.prefs.model.AppSettings
import com.example.webviewbrowser.data.prefs.model.CookiePolicy

/**
 * AppSettings を WebView の WebSettings に反映する。
 *
 * 新規 WebView 生成時、および設定変更時に呼ぶ（既存 WebView へ即時反映）。
 */
object WebSettingsApplier {

    fun apply(webView: WebView, settings: AppSettings) {
        webView.settings.apply {
            javaScriptEnabled = settings.javascriptEnabled
            javaScriptCanOpenWindowsAutomatically = !settings.blockPopups
            setSupportMultipleWindows(!settings.blockPopups)
            textZoom = settings.textScaling
        }
        val acceptCookies = settings.cookiePolicy == CookiePolicy.ALL
        CookieManager.getInstance().setAcceptCookie(acceptCookies)
        // third-party cookie も設定連動させる。
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, acceptCookies)
    }
}
