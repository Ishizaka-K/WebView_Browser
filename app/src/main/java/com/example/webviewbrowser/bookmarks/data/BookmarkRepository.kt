package com.example.webviewbrowser.bookmarks.data

import com.example.webviewbrowser.data.db.dao.BookmarkDao
import com.example.webviewbrowser.data.db.dao.BookmarkFolderDao
import com.example.webviewbrowser.data.db.entity.BookmarkEntity
import com.example.webviewbrowser.data.db.entity.BookmarkFolderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** ブックマークとフォルダの永続化をラップする。 */
@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val folderDao: BookmarkFolderDao,
) {
    fun observeBookmarks(folderId: String?): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeInFolder(folderId)

    fun observeFolders(parentId: String?): Flow<List<BookmarkFolderEntity>> =
        folderDao.observeChildren(parentId)

    fun observeAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()

    suspend fun getBookmarkById(id: String): BookmarkEntity? = bookmarkDao.getById(id)
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity? = bookmarkDao.getByUrl(url)
    suspend fun upsertBookmark(b: BookmarkEntity) = bookmarkDao.upsert(b)
    suspend fun deleteBookmark(id: String) = bookmarkDao.deleteById(id)

    suspend fun getFolder(id: String): BookmarkFolderEntity? = folderDao.getById(id)
    suspend fun upsertFolder(f: BookmarkFolderEntity) = folderDao.upsert(f)
    suspend fun deleteFolder(id: String) = folderDao.deleteById(id)
}
