package com.example.webviewbrowser.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.webviewbrowser.data.db.entity.BookmarkEntity
import com.example.webviewbrowser.data.db.entity.BookmarkFolderEntity
import com.example.webviewbrowser.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `bookmark folder cascade delete removes children`() = runTest {
        val folderDao = db.bookmarkFolderDao()
        val bookmarkDao = db.bookmarkDao()

        val root = BookmarkFolderEntity("f1", "root", null, 0, 0)
        val child = BookmarkFolderEntity("f2", "child", "f1", 0, 0)
        folderDao.upsert(root)
        folderDao.upsert(child)
        bookmarkDao.upsert(BookmarkEntity("b1", "t", "https://a.com", null, "f2", 0, 0))

        // ルート削除で子フォルダと配下ブックマークが CASCADE 削除される。
        folderDao.deleteById("f1")

        assertNull(folderDao.getById("f2"))
        assertNull(bookmarkDao.getById("b1"))
    }

    @Test
    fun `recordVisit inserts then increments atomically`() = runTest {
        val historyDao = db.historyDao()
        historyDao.recordVisit("https://a.com", "A", 1000)
        historyDao.recordVisit("https://a.com", "A2", 2000)

        val updated = historyDao.getByUrl("https://a.com")
        assertEquals(2, updated?.visitCount)
        assertEquals(2000L, updated?.visitedAt)
        assertEquals("A2", updated?.title)
    }

    @Test
    fun `history most visited ordered by count`() = runTest {
        val dao = db.historyDao()
        dao.upsert(HistoryEntity("h1", "https://a.com", "A", 1000, 1))
        dao.upsert(HistoryEntity("h2", "https://b.com", "B", 1000, 5))

        val top = dao.observeMostVisited(10).first()
        assertEquals("https://b.com", top.first().url)
    }
}
