package com.example.webviewbrowser.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ダウンロードのメタデータ。
 *
 * - 一時ファイルは [tempPath]（アプリ専用領域の .part）。
 * - 完了時に MediaStore へコピーし [savedUri] を設定する。
 * - レジューム用に [etag]/[lastModified] を保持。
 * - プロセス再起動後の再開用に [userAgent]/[referer] を保持（Cookie は保存せず再取得）。
 */
@Entity(
    tableName = "downloads",
    indices = [Index("status"), Index("createdAt")],
)
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val etag: String?,
    val lastModified: String?,
    val tempPath: String,
    val destinationType: DestinationType,
    val relativePath: String?,
    val savedUri: String?,
    val sourcePageUrl: String?,
    val userAgent: String?,
    val referer: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
