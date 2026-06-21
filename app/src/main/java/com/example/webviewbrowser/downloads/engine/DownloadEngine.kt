package com.example.webviewbrowser.downloads.engine

import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.webviewbrowser.data.db.entity.DestinationType
import com.example.webviewbrowser.data.db.entity.DownloadEntity
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.downloads.data.DownloadRepository
import com.example.webviewbrowser.downloads.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ダウンロードのキュー管理 API。
 * 実行は DownloadService が所有する。Engine は DB 更新と Service へのコマンド送出に限定する。
 */
interface DownloadEngine {
    suspend fun enqueue(request: DownloadRequest): String
    suspend fun pause(id: String)
    suspend fun resume(id: String)
    suspend fun cancel(id: String)
    suspend fun retry(id: String)
    fun observeAll(): Flow<List<DownloadProgress>>
}

@Singleton
class DefaultDownloadEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository,
    private val fileStore: FileStore,
) : DownloadEngine {

    override suspend fun enqueue(request: DownloadRequest): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val destination = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DestinationType.MEDIA_STORE_DOWNLOADS
        } else {
            DestinationType.APP_PRIVATE
        }
        repository.upsert(
            DownloadEntity(
                id = id,
                url = request.url,
                fileName = request.fileName,
                mimeType = request.mimeType,
                totalBytes = request.contentLength.coerceAtLeast(0),
                downloadedBytes = 0,
                status = DownloadStatus.QUEUED,
                etag = null,
                lastModified = null,
                tempPath = fileStore.partFile(id).absolutePath,
                destinationType = destination,
                relativePath = Environment.DIRECTORY_DOWNLOADS,
                savedUri = null,
                sourcePageUrl = request.sourcePageUrl,
                userAgent = request.userAgent,
                referer = request.referer,
                createdAt = now,
                updatedAt = now,
            ),
        )
        DownloadService.start(context, DownloadService.ACTION_START)
        return id
    }

    override suspend fun pause(id: String) {
        val status = repository.get(id)?.status ?: return
        if (DownloadTransitionPolicy.canPause(status)) {
            DownloadService.start(context, DownloadService.ACTION_PAUSE, id)
        }
    }

    override suspend fun resume(id: String) {
        val status = repository.get(id)?.status ?: return
        if (!DownloadTransitionPolicy.canResume(status)) return
        repository.updateStatus(id, DownloadStatus.QUEUED)
        DownloadService.start(context, DownloadService.ACTION_START)
    }

    override suspend fun cancel(id: String) {
        val status = repository.get(id)?.status ?: return
        if (DownloadTransitionPolicy.canCancel(status)) {
            DownloadService.start(context, DownloadService.ACTION_CANCEL, id)
        }
    }

    override suspend fun retry(id: String) {
        val status = repository.get(id)?.status ?: return
        if (!DownloadTransitionPolicy.canRetry(status)) return
        repository.updateStatus(id, DownloadStatus.QUEUED)
        DownloadService.start(context, DownloadService.ACTION_START)
    }

    override fun observeAll(): Flow<List<DownloadProgress>> = repository.observeAll()
}

/** UI・通知の古い操作が完了済みデータを巻き戻さないための状態遷移規則。 */
internal object DownloadTransitionPolicy {
    fun canPause(status: DownloadStatus): Boolean =
        status == DownloadStatus.RUNNING || status == DownloadStatus.QUEUED

    fun canResume(status: DownloadStatus): Boolean = status == DownloadStatus.PAUSED

    fun canRetry(status: DownloadStatus): Boolean =
        status == DownloadStatus.FAILED || status == DownloadStatus.CANCELED

    fun canCancel(status: DownloadStatus): Boolean =
        status == DownloadStatus.RUNNING || status == DownloadStatus.QUEUED || status == DownloadStatus.PAUSED
}
