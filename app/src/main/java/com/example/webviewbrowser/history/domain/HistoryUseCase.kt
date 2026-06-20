package com.example.webviewbrowser.history.domain

import com.example.webviewbrowser.core.port.HistoryRecorder
import com.example.webviewbrowser.data.db.entity.HistoryEntity
import com.example.webviewbrowser.history.data.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 履歴の記録/閲覧/検索/削除。
 * [HistoryRecorder] を実装し、ブラウザコア(Task5)からの記録要求を受ける。
 */
@Singleton
class HistoryUseCase @Inject constructor(
    private val repository: HistoryRepository,
) : HistoryRecorder {

    override suspend fun record(url: String, title: String) {
        // hash-only 遷移を重複記録しないよう fragment を除去して保存する。
        val normalized = url.substringBefore("#")
        if (normalized.isBlank()) return
        repository.record(normalized, title)
    }

    fun observeAll(): Flow<List<HistoryEntity>> = repository.observeAll()
    fun search(query: String): Flow<List<HistoryEntity>> = repository.search(query)
    fun mostVisited(limit: Int): Flow<List<HistoryEntity>> = repository.observeMostVisited(limit)

    suspend fun delete(id: String) = repository.deleteById(id)
    suspend fun clearAll() = repository.clearAll()
}
