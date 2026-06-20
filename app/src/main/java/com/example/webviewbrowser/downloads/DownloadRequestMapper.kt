package com.example.webviewbrowser.downloads

import android.webkit.URLUtil
import com.example.webviewbrowser.browser.webview.WebViewDownloadEvent
import com.example.webviewbrowser.downloads.engine.DownloadRequest

/** WebView 由来の DL イベントを downloads ドメインの DownloadRequest に変換する。 */
object DownloadRequestMapper {

    fun map(event: WebViewDownloadEvent): DownloadRequest {
        val fileName = URLUtil.guessFileName(event.url, event.contentDisposition, event.mimeType)
        return DownloadRequest(
            url = event.url,
            fileName = fileName,
            mimeType = event.mimeType,
            contentLength = event.contentLength,
            sourcePageUrl = event.sourcePageUrl,
            userAgent = event.userAgent,
            cookieHeader = event.cookieHeader,
            referer = event.referer,
        )
    }
}
