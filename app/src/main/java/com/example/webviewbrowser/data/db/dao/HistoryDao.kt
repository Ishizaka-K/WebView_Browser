package com.example.webviewbrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.webviewbrowser.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface HistoryDao {

    @Upsert
    suspend fun upsert(entry: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): HistoryEntity?

    /** 既存 URL の訪問回数を原子的に加算する。更新件数を返す。 */
    @Query(
        "UPDATE history SET title = :title, visitedAt = :visitedAt, visitCount = visitCount + 1 " +
            "WHERE url = :url",
    )
    suspend fun incrementVisit(url: String, title: String, visitedAt: Long): Int

    /**
     * 訪問を記録する。既存があれば加算、なければ追加する。
     * read-modify-write の競合を避けるため Transaction + 原子 UPDATE を使う。
     */
    @Transaction
    suspend fun recordVisit(url: String, title: String, visitedAt: Long) {
        val updated = incrementVisit(url, title, visitedAt)
        if (updated == 0) {
            insert(
                HistoryEntity(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    title = title,
                    visitedAt = visitedAt,
                    visitCount = 1,
                ),
            )
        }
    }

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query(
        "SELECT * FROM history WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' " +
            "ORDER BY visitedAt DESC",
    )
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitCount DESC, visitedAt DESC LIMIT :limit")
    fun observeMostVisited(limit: Int): Flow<List<HistoryEntity>>
}
