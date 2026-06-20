package com.example.webviewbrowser.downloads.engine

import android.webkit.CookieManager
import com.example.webviewbrowser.data.db.entity.DownloadEntity
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.downloads.data.DownloadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * 1 件のダウンロードを OkHttp + Range で実行する。
 *
 * レジューム規約:
 * - 常に `Accept-Encoding: identity`。
 * - downloadedBytes>0 のとき `Range` と `If-Range` を送る。
 * - 206 は Content-Range の開始位置一致を必須。
 * - 200 は .part を破棄して先頭から。
 * - 416 は保存済みサイズと total を照合。
 */
@Singleton
class DownloadTaskRunner @Inject constructor(
    private val client: OkHttpClient,
    private val repository: DownloadRepository,
    private val fileStore: FileStore,
) {
    /** 実行結果。 */
    enum class Result { COMPLETED, PAUSED, FAILED }

    /**
     * @param cookieHeader 認証 DL 用の Cookie。永続化しないため実行時に渡す
     *   （プロセス再起動後の再開時は null になりうる）。
     */
    suspend fun run(id: String, cookieHeader: String?): Result = withContext(Dispatchers.IO) {
        val entity = repository.get(id) ?: return@withContext Result.FAILED
        try {
            repository.updateStatus(id, DownloadStatus.RUNNING)
            execute(entity, cookieHeader)
        } catch (e: CancellationException) {
            // pause/cancel 起因。状態は呼び出し側(Executor)が確定する。
            throw e
        } catch (e: Exception) {
            repository.updateStatus(id, DownloadStatus.FAILED)
            Result.FAILED
        }
    }

    private suspend fun execute(entity: DownloadEntity, cookieHeader: String?): Result {
        val part = fileStore.partFile(entity.id)
        var existing = if (part.exists()) part.length() else 0L
        if (existing > entity.totalBytes && entity.totalBytes > 0) {
            // 不整合: .part が大きすぎる。破棄して先頭から。
            part.delete()
            existing = 0L
        }

        val requestBuilder = Request.Builder()
            .url(entity.url)
            .header("Accept-Encoding", "identity")
        entity.userAgent?.let { requestBuilder.header("User-Agent", it) }
        entity.referer?.let { requestBuilder.header("Referer", it) }
        // Cookie は永続化しない。実行時引数 → なければ CookieManager から再取得。
        val cookie = cookieHeader?.takeIf { it.isNotBlank() }
            ?: runCatching { CookieManager.getInstance().getCookie(entity.url) }.getOrNull()
        cookie?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Cookie", it) }
        if (existing > 0) {
            requestBuilder.header("Range", "bytes=$existing-")
            (entity.etag ?: entity.lastModified)?.let { requestBuilder.header("If-Range", it) }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            var total = entity.totalBytes
            when (response.code) {
                416 -> {
                    // Range 範囲外: 既存サイズ == total なら完了扱い。
                    return if (entity.totalBytes > 0 && existing >= entity.totalBytes) {
                        finish(entity, expectedTotal = entity.totalBytes)
                    } else {
                        repository.updateStatus(entity.id, DownloadStatus.FAILED)
                        Result.FAILED
                    }
                }
                200 -> {
                    // Range 非対応。先頭から取り直す。
                    part.delete()
                    existing = 0L
                }
                206 -> {
                    val cr = parseContentRange(response.header("Content-Range"))
                    if (cr == null || cr.start != existing) {
                        repository.updateStatus(entity.id, DownloadStatus.FAILED)
                        return Result.FAILED
                    }
                    if (cr.total > 0) total = cr.total
                }
                else -> {
                    repository.updateStatus(entity.id, DownloadStatus.FAILED)
                    return Result.FAILED
                }
            }

            val body = response.body ?: run {
                repository.updateStatus(entity.id, DownloadStatus.FAILED)
                return Result.FAILED
            }

            // validator と total を保存して以降のレジュームに備える。
            val etag = response.header("ETag")
            val lastModified = response.header("Last-Modified")
            val reportedLength = body.contentLength()
            if (total <= 0) {
                total = when {
                    response.code == 206 && reportedLength > 0 -> existing + reportedLength
                    reportedLength > 0 -> reportedLength
                    else -> 0L
                }
            }
            repository.upsert(
                entity.copy(
                    totalBytes = total,
                    downloadedBytes = existing,
                    etag = etag ?: entity.etag,
                    lastModified = lastModified ?: entity.lastModified,
                ),
            )

            var downloaded = existing
            RandomAccessFile(part, "rw").use { raf ->
                raf.seek(existing)
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var lastReport = 0L
                    while (true) {
                        coroutineContext.ensureActive() // pause/cancel で CancellationException
                        val read = input.read(buffer)
                        if (read == -1) break
                        raf.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded - lastReport >= REPORT_THRESHOLD) {
                            repository.updateProgress(entity.id, downloaded, DownloadStatus.RUNNING)
                            lastReport = downloaded
                        }
                    }
                }
            }
            repository.updateProgress(entity.id, downloaded, DownloadStatus.RUNNING)

            // 完了検証: total が既知ならファイル長と一致を必須にする（破損防止）。
            val actualLength = part.length()
            if (total > 0 && actualLength != total) {
                repository.updateStatus(entity.id, DownloadStatus.FAILED)
                return Result.FAILED
            }
            return finish(entity, expectedTotal = if (total > 0) total else actualLength)
        }
    }

    private suspend fun finish(entity: DownloadEntity, expectedTotal: Long): Result {
        val savedUri = fileStore.publish(entity.id, entity.fileName, entity.mimeType)
        val now = System.currentTimeMillis()
        val latest = repository.get(entity.id) ?: entity
        repository.upsert(
            latest.copy(
                status = DownloadStatus.COMPLETED,
                totalBytes = expectedTotal,
                downloadedBytes = expectedTotal,
                savedUri = savedUri,
                updatedAt = now,
            ),
        )
        return Result.COMPLETED
    }

    private data class ContentRange(val start: Long, val end: Long, val total: Long)

    private fun parseContentRange(header: String?): ContentRange? {
        // 例: "bytes 200-1023/1024" / total 不明は "*"
        if (header == null) return null
        val spec = header.substringAfter("bytes ", "").trim()
        val rangePart = spec.substringBefore("/", "")
        val totalPart = spec.substringAfter("/", "")
        val start = rangePart.substringBefore("-", "").toLongOrNull() ?: return null
        val end = rangePart.substringAfter("-", "").toLongOrNull() ?: return null
        val total = totalPart.toLongOrNull() ?: -1L
        return ContentRange(start, end, total)
    }

    companion object {
        private const val REPORT_THRESHOLD = 256 * 1024L
    }
}
