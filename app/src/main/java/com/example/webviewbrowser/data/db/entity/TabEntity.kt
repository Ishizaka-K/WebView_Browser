package com.example.webviewbrowser.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * セッション復元用のタブ情報。
 * [contentType] が HOME の場合 url は home sentinel を保持しうる。
 */
@Entity(
    tableName = "tabs",
    indices = [Index("position")],
)
data class TabEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val position: Int,
    val isActive: Boolean,
    val contentType: TabContentType,
    val updatedAt: Long,
)
