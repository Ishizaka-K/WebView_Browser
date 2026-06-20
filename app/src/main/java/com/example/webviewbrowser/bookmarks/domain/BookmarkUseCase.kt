package com.example.webviewbrowser.bookmarks.domain

import com.example.webviewbrowser.bookmarks.data.BookmarkRepository
import com.example.webviewbrowser.data.db.entity.BookmarkEntity
import com.example.webviewbrowser.data.db.entity.BookmarkFolderEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** ブックマーク操作。多層フォルダと循環参照防止を含む。 */
@Singleton
class BookmarkUseCase @Inject constructor(
    private val repository: BookmarkRepository,
) {
    suspend fun addBookmark(title: String, url: String, parentFolderId: String? = null) {
        val now = System.currentTimeMillis()
        repository.upsertBookmark(
            BookmarkEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                url = url,
                faviconUrl = null,
                parentFolderId = parentFolderId,
                createdAt = now,
                position = now.toInt(),
            ),
        )
    }

    suspend fun removeBookmark(id: String) = repository.deleteBookmark(id)

    suspend fun isBookmarked(url: String): Boolean = repository.getBookmarkByUrl(url) != null

    /** URL のブックマーク有無をトグルする。追加時は title を使う。 */
    suspend fun toggle(title: String, url: String, parentFolderId: String? = null) {
        val existing = repository.getBookmarkByUrl(url)
        if (existing != null) repository.deleteBookmark(existing.id)
        else addBookmark(title, url, parentFolderId)
    }

    suspend fun createFolder(name: String, parentFolderId: String? = null) {
        val now = System.currentTimeMillis()
        repository.upsertFolder(
            BookmarkFolderEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                parentFolderId = parentFolderId,
                createdAt = now,
                position = now.toInt(),
            ),
        )
    }

    suspend fun renameFolder(id: String, name: String) {
        val folder = repository.getFolder(id) ?: return
        repository.upsertFolder(folder.copy(name = name))
    }

    /** フォルダ削除（配下は FK CASCADE で削除）。 */
    suspend fun deleteFolder(id: String) = repository.deleteFolder(id)

    suspend fun moveBookmark(id: String, targetFolderId: String?) {
        val bookmark = repository.getBookmarkById(id) ?: return
        repository.upsertBookmark(bookmark.copy(parentFolderId = targetFolderId))
    }

    /**
     * フォルダを移動する。移動先が自分自身または子孫なら拒否（循環防止）。
     * @return 移動できたら true。
     */
    suspend fun moveFolder(id: String, targetFolderId: String?): Boolean {
        if (id == targetFolderId) return false
        if (targetFolderId != null && isDescendant(ancestorCandidate = id, of = targetFolderId)) {
            // targetFolderId が id の子孫なら循環になる。
            return false
        }
        val folder = repository.getFolder(id) ?: return false
        repository.upsertFolder(folder.copy(parentFolderId = targetFolderId))
        return true
    }

    /** [of] の祖先チェーンに [ancestorCandidate] が含まれるか。 */
    private suspend fun isDescendant(ancestorCandidate: String, of: String): Boolean {
        var current: String? = of
        // 祖先をたどり、ancestorCandidate に当たれば of は ancestorCandidate の子孫。
        while (current != null) {
            if (current == ancestorCandidate) return true
            current = repository.getFolder(current)?.parentFolderId
        }
        return false
    }
}
