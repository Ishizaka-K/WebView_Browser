package com.example.webviewbrowser.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 多層フォルダのためのブックマークフォルダ。
 *
 * 自己参照 FK。[parentFolderId] が null はルート直下。
 * 親フォルダ削除時は CASCADE で配下フォルダも削除される。
 */
@Entity(
    tableName = "bookmark_folders",
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
data class BookmarkFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentFolderId: String?,
    val createdAt: Long,
    val position: Int,
)
