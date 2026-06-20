package com.example.webviewbrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.webviewbrowser.data.db.entity.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {

    @Upsert
    suspend fun upsert(tab: TabEntity)

    @Upsert
    suspend fun upsertAll(tabs: List<TabEntity>)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tabs")
    suspend fun clear()

    @Query("SELECT * FROM tabs ORDER BY position ASC")
    suspend fun getAll(): List<TabEntity>

    @Query("SELECT * FROM tabs ORDER BY position ASC")
    fun observeAll(): Flow<List<TabEntity>>

    /** セッション全体を置き換える。 */
    @Transaction
    suspend fun replaceAll(tabs: List<TabEntity>) {
        clear()
        upsertAll(tabs)
    }
}
