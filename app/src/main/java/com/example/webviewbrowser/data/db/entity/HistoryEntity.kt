package com.example.webviewbrowser.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 閲覧履歴。URL を一意キーとし、再訪問で [visitCount] を加算する。
 */
@Entity(
    tableName = "history",
    indices = [Index(value = ["url"], unique = true), Index("visitedAt")],
)
data class HistoryEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val visitedAt: Long,
    val visitCount: Int,
)
