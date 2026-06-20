package com.example.webviewbrowser.history.data

import com.example.webviewbrowser.data.db.dao.HistoryDao
import com.example.webviewbrowser.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** HistoryDao をラップする。 */
@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao,
) {
    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()
    fun search(query: String): Flow<List<HistoryEntity>> = dao.search(query)
    fun observeMostVisited(limit: Int): Flow<List<HistoryEntity>> = dao.observeMostVisited(limit)

    suspend fun record(url: String, title: String) =
        dao.recordVisit(url, title, System.currentTimeMillis())

    suspend fun deleteById(id: String) = dao.deleteById(id)
    suspend fun clearAll() = dao.clearAll()
}
