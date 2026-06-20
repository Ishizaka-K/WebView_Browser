package com.example.webviewbrowser.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.webviewbrowser.data.db.AppDatabase
import com.example.webviewbrowser.history.data.HistoryRepository
import com.example.webviewbrowser.history.domain.HistoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var useCase: HistoryUseCase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        useCase = HistoryUseCase(HistoryRepository(db.historyDao()))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `record then search and clear`() = runTest {
        useCase.record("https://a.com", "Alpha")
        useCase.record("https://b.com", "Beta")

        val all = useCase.observeAll().first()
        assertEquals(2, all.size)

        val found = useCase.search("Alpha").first()
        assertEquals(1, found.size)
        assertEquals("https://a.com", found.first().url)

        useCase.clearAll()
        assertTrue(useCase.observeAll().first().isEmpty())
    }
}
