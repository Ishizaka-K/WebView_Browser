package com.example.webviewbrowser.bookmarks

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.webviewbrowser.bookmarks.data.BookmarkRepository
import com.example.webviewbrowser.bookmarks.domain.BookmarkUseCase
import com.example.webviewbrowser.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookmarkUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BookmarkRepository
    private lateinit var useCase: BookmarkUseCase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = BookmarkRepository(db.bookmarkDao(), db.bookmarkFolderDao())
        useCase = BookmarkUseCase(repository)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `toggle adds then removes bookmark`() = runTest {
        useCase.toggle("Title", "https://a.com")
        assertTrue(useCase.isBookmarked("https://a.com"))
        useCase.toggle("Title", "https://a.com")
        assertFalse(useCase.isBookmarked("https://a.com"))
    }

    @Test
    fun `moving folder into its own descendant is rejected`() = runTest {
        // root -> A -> B
        useCase.createFolder("A")
        val a = repository.observeFolders(null).first().first()
        useCase.createFolder("B", a.id)
        val b = repository.observeFolders(a.id).first().first()

        // A を B(子孫) の下に移動しようとすると拒否される。
        val moved = useCase.moveFolder(a.id, b.id)
        assertFalse(moved)
    }

    @Test
    fun `moving folder to unrelated target succeeds`() = runTest {
        useCase.createFolder("A")
        useCase.createFolder("C")
        val folders = repository.observeFolders(null).first()
        val a = folders.first { it.name == "A" }
        val c = folders.first { it.name == "C" }

        val moved = useCase.moveFolder(a.id, c.id)
        assertTrue(moved)
    }

    @Test
    fun `folder delete cascades bookmarks`() = runTest {
        useCase.createFolder("A")
        val a = repository.observeFolders(null).first().first()
        useCase.addBookmark("t", "https://x.com", a.id)
        assertTrue(useCase.isBookmarked("https://x.com"))

        useCase.deleteFolder(a.id)
        assertFalse(useCase.isBookmarked("https://x.com"))
    }
}
