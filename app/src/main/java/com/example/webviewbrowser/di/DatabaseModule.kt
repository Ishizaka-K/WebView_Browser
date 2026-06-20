package com.example.webviewbrowser.di

import android.content.Context
import androidx.room.Room
import com.example.webviewbrowser.data.db.AppDatabase
import com.example.webviewbrowser.data.db.dao.BookmarkDao
import com.example.webviewbrowser.data.db.dao.BookmarkFolderDao
import com.example.webviewbrowser.data.db.dao.DownloadDao
import com.example.webviewbrowser.data.db.dao.HistoryDao
import com.example.webviewbrowser.data.db.dao.TabDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 永続化層の DI。AppDatabase / DAO を提供する。
 * feature ごとの Repository binding は各 feature の *FeatureModule が所有する。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Room は FK を定義したエンティティに対し既定で foreign_keys=ON にする。
            // 本 DB は CASCADE 削除に依存する（bookmark_folders/bookmarks）。
            .build()

    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideBookmarkFolderDao(db: AppDatabase): BookmarkFolderDao = db.bookmarkFolderDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideTabDao(db: AppDatabase): TabDao = db.tabDao()
}
