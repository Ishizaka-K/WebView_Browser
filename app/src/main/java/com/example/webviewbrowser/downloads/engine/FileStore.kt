package com.example.webviewbrowser.downloads.engine

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 一時ファイル(.part)管理と、完了時の公開（MediaStore Downloads / 旧 API は外部 Downloads）。
 *
 * - 一時ファイルは常にアプリ専用領域 `filesDir/downloads/{id}.part`。
 * - 完了後に公開先へコピーし、`.part` を削除する。
 */
@Singleton
open class FileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val partDir: File by lazy {
        File(context.filesDir, "downloads").apply { mkdirs() }
    }

    open fun partFile(id: String): File = File(partDir, "$id.part")

    /**
     * 完了した .part を公開先へコピーする。成功で savedUri 文字列を返す。
     */
    open suspend fun publish(id: String, fileName: String, mimeType: String?): String =
        withContext(Dispatchers.IO) {
            val part = partFile(id)
            require(part.exists()) { "part file not found for $id" }
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                publishViaMediaStore(part, fileName, mimeType)
            } else {
                publishToLegacyDownloads(part, fileName)
            }
            part.delete()
            uri
        }

    private fun publishViaMediaStore(part: File, fileName: String, mimeType: String?): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            if (!mimeType.isNullOrBlank()) put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val itemUri = resolver.insert(collection, values)
            ?: error("Failed to create MediaStore entry")
        resolver.openOutputStream(itemUri)?.use { out ->
            part.inputStream().use { it.copyTo(out) }
        } ?: error("Failed to open output stream")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(itemUri, values, null, null)
        return itemUri.toString()
    }

    /**
     * API 26-28: アプリ専用外部 Downloads に保存（権限不要）。
     * 他アプリへ渡せるよう FileProvider の content:// URI を返す（FileUriExposedException 回避）。
     */
    private fun publishToLegacyDownloads(part: File, fileName: String): String {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads").apply { mkdirs() }
        val dest = File(dir, fileName)
        part.copyTo(dest, overwrite = true)
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, dest).toString()
    }
}
