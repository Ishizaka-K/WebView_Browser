package com.example.webviewbrowser.data.prefs

import androidx.test.core.app.ApplicationProvider
import com.example.webviewbrowser.data.prefs.model.CookiePolicy
import com.example.webviewbrowser.data.prefs.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsDataStoreTest {

    private fun newStore() =
        SettingsDataStore(ApplicationProvider.getApplicationContext())

    @Test
    fun `defaults are returned when empty`() = runTest {
        val settings = newStore().settings.first()
        assertEquals("google", settings.searchEngineId)
        assertEquals(100, settings.textScaling)
        assertEquals(3, settings.maxParallelDownloads)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(CookiePolicy.ALL, settings.cookiePolicy)
    }

    @Test
    fun `setters persist values`() = runTest {
        val store = newStore()
        store.setJavascriptEnabled(false)
        store.setThemeMode(ThemeMode.DARK)
        val settings = store.settings.first()
        assertEquals(false, settings.javascriptEnabled)
        assertEquals(ThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun `text scaling and parallel downloads are coerced`() = runTest {
        val store = newStore()
        store.setTextScaling(5000)
        store.setMaxParallelDownloads(0)
        val settings = store.settings.first()
        assertEquals(SettingsDataStore.MAX_TEXT_SCALING, settings.textScaling)
        assertEquals(1, settings.maxParallelDownloads)
    }
}
