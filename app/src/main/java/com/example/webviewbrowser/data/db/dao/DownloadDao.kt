package com.example.webviewbrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.webviewbrowser.data.db.entity.DownloadEntity
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE downloads SET downloadedBytes = :bytes, status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: String, bytes: Long, status: DownloadStatus, updatedAt: Long)

    @Query("UPDATE downloads SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus, updatedAt: Long)

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN (:statuses)")
    suspend fun getByStatuses(statuses: List<DownloadStatus>): List<DownloadEntity>

    @Query("UPDATE downloads SET status = :to, updatedAt = :updatedAt WHERE status = :from")
    suspend fun normalizeStatus(from: DownloadStatus, to: DownloadStatus, updatedAt: Long)
}
