package com.example.webviewbrowser.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.webviewbrowser.data.db.dao.BookmarkDao
import com.example.webviewbrowser.data.db.dao.BookmarkFolderDao
import com.example.webviewbrowser.data.db.dao.DownloadDao
import com.example.webviewbrowser.data.db.dao.HistoryDao
import com.example.webviewbrowser.data.db.dao.TabDao
import com.example.webviewbrowser.data.db.entity.BookmarkEntity
import com.example.webviewbrowser.data.db.entity.BookmarkFolderEntity
import com.example.webviewbrowser.data.db.entity.DownloadEntity
import com.example.webviewbrowser.data.db.entity.HistoryEntity
import com.example.webviewbrowser.data.db.entity.TabEntity

@Database(
    entities = [
        BookmarkFolderEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
        TabEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun bookmarkFolderDao(): BookmarkFolderDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun tabDao(): TabDao

    companion object {
        const val NAME = "webviewbrowser.db"
    }
}
