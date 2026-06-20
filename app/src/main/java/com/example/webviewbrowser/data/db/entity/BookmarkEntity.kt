package com.example.webviewbrowser.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ブックマーク。[parentFolderId] が null はルート直下。
 * 所属フォルダ削除時は CASCADE で削除される。
 */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookmarkFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parentFolderId")],
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val faviconUrl: String?,
    val parentFolderId: String?,
    val createdAt: Long,
    val position: Int,
)
