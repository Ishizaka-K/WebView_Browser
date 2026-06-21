package com.example.webviewbrowser.downloads.data

import com.example.webviewbrowser.data.db.dao.DownloadDao
import com.example.webviewbrowser.data.db.entity.DownloadEntity
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.downloads.engine.DownloadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DownloadDao をラップし、UI 向けの Flow を公開する。 */
@Singleton
class DownloadRepository @Inject constructor(
    private val dao: DownloadDao,
) {
    fun observeAll(): Flow<List<DownloadProgress>> =
        dao.observeAll().map { list -> list.map { it.toProgress() } }

    suspend fun get(id: String): DownloadEntity? = dao.getById(id)

    suspend fun upsert(entity: DownloadEntity) = dao.upsert(entity)

    suspend fun updateProgress(id: String, bytes: Long, status: DownloadStatus) =
        dao.updateProgress(id, bytes, status, System.currentTimeMillis())

    suspend fun updateStatus(id: String, status: DownloadStatus) =
        dao.updateStatus(id, status, System.currentTimeMillis())

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun getByStatuses(statuses: List<DownloadStatus>): List<DownloadEntity> =
        dao.getByStatuses(statuses)

    /** 起動時、RUNNING を PAUSED に正規化する（プロセス自動再起動はしない）。 */
    suspend fun normalizeRunningToPaused() =
        dao.normalizeStatus(DownloadStatus.RUNNING, DownloadStatus.PAUSED, System.currentTimeMillis())

    private fun DownloadEntity.toProgress() = DownloadProgress(
        id = id,
        fileName = fileName,
        url = url,
        mimeType = mimeType,
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        status = status,
        savedUri = savedUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
