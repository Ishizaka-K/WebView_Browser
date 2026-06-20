package com.example.webviewbrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.webviewbrowser.data.db.entity.BookmarkFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkFolderDao {

    @Upsert
    suspend fun upsert(folder: BookmarkFolderEntity)

    @Delete
    suspend fun delete(folder: BookmarkFolderEntity)

    @Query("DELETE FROM bookmark_folders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM bookmark_folders WHERE id = :id")
    suspend fun getById(id: String): BookmarkFolderEntity?

    @Query(
        "SELECT * FROM bookmark_folders WHERE " +
            "(:parentFolderId IS NULL AND parentFolderId IS NULL) OR parentFolderId = :parentFolderId " +
            "ORDER BY position ASC",
    )
    fun observeChildren(parentFolderId: String?): Flow<List<BookmarkFolderEntity>>

    @Query("SELECT * FROM bookmark_folders ORDER BY position ASC")
    fun observeAll(): Flow<List<BookmarkFolderEntity>>
}
