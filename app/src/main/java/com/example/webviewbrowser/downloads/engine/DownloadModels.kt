package com.example.webviewbrowser.downloads.engine

import com.example.webviewbrowser.data.db.entity.DownloadStatus

/**
 * ダウンロード要求（downloads ドメインの型）。
 * WebView 由来の WebViewDownloadEvent からは統合層(Task11)が変換する。
 */
data class DownloadRequest(
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val contentLength: Long,
    val sourcePageUrl: String?,
    val userAgent: String?,
    val cookieHeader: String?,
    val referer: String?,
)

/** UI へ公開する進捗スナップショット。 */
data class DownloadProgress(
    val id: String,
    val fileName: String,
    val url: String,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val savedUri: String?,
) {
    val percent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}
