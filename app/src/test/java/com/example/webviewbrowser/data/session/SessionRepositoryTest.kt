package com.example.webviewbrowser.data.session

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.webviewbrowser.data.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SessionRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = SessionRepository(db.tabDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `save and restore preserves order and active and home`() = runTest {
        val tabs = listOf(
            SessionTab("t1", "https://a.com", "A", isActive = false, isHome = false),
            SessionTab("t2", "home://start", "Home", isActive = true, isHome = true),
        )
        repository.save(tabs)

        val restored = repository.restore()
        assertEquals(2, restored.size)
        assertEquals("t1", restored[0].id)
        assertTrue(restored[1].isHome)
        assertTrue(restored[1].isActive)
    }

    @Test
    fun `save replaces previous session`() = runTest {
        repository.save(listOf(SessionTab("t1", "https://a.com", "A", false, false)))
        repository.save(listOf(SessionTab("t2", "https://b.com", "B", true, false)))

        val restored = repository.restore()
        assertEquals(1, restored.size)
        assertEquals("t2", restored[0].id)
    }
}
