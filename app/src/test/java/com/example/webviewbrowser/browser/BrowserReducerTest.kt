package com.example.webviewbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserReducerTest {

    private fun homeTab(id: String, active: Boolean = false) =
        TabUiState(id = id, url = BrowserReducer.HOME, title = "Home", isHome = true)

    private fun stateWith(vararg tabs: TabUiState, activeId: String? = tabs.firstOrNull()?.id) =
        BrowserState(tabs = tabs.toList(), activeTabId = activeId)

    @Test
    fun `page started marks loading and not home`() {
        val state = stateWith(homeTab("t1"))
        val result = BrowserReducer.reduce(state, BrowserIntent.PageStarted("t1", "https://a.com"))
        val tab = result.tabs.first()
        assertTrue(tab.isLoading)
        assertFalse(tab.isHome)
        assertEquals("https://a.com", tab.url)
    }

    @Test
    fun `page finished sets nav flags and syncs address for active tab`() {
        val state = stateWith(homeTab("t1"))
        val result = BrowserReducer.reduce(
            state,
            BrowserIntent.PageFinished("t1", "https://a.com/x", canGoBack = true, canGoForward = false),
        )
        val tab = result.tabs.first()
        assertEquals(100, tab.progress)
        assertFalse(tab.isLoading)
        assertTrue(tab.canGoBack)
        assertEquals("https://a.com/x", result.addressInput)
    }

    @Test
    fun `progress changed updates loading state`() {
        val state = stateWith(homeTab("t1"))
        val mid = BrowserReducer.reduce(state, BrowserIntent.ProgressChanged("t1", 40))
        assertTrue(mid.tabs.first().isLoading)
        val done = BrowserReducer.reduce(state, BrowserIntent.ProgressChanged("t1", 100))
        assertFalse(done.tabs.first().isLoading)
    }

    @Test
    fun `new tab can be added and activated`() {
        val state = stateWith(homeTab("t1"))
        val newTab = homeTab("t2")
        val result = BrowserReducer.withNewTab(state, newTab, activate = true)
        assertEquals(2, result.tabs.size)
        assertEquals("t2", result.activeTabId)
    }

    @Test
    fun `closing active tab activates neighbor`() {
        val state = stateWith(homeTab("t1"), homeTab("t2"), activeId = "t2")
        val result = BrowserReducer.withClosedTab(state, "t2") { homeTab("new") }
        assertEquals(1, result.tabs.size)
        assertEquals("t1", result.activeTabId)
    }

    @Test
    fun `closing last tab creates a new home tab`() {
        val state = stateWith(homeTab("t1"))
        val result = BrowserReducer.withClosedTab(state, "t1") { homeTab("new") }
        assertEquals(1, result.tabs.size)
        assertEquals("new", result.activeTabId)
        assertTrue(result.tabs.first().isHome)
    }

    @Test
    fun `switch tab updates active and address`() {
        val web = TabUiState("t2", "https://b.com", "B", isHome = false)
        val state = stateWith(homeTab("t1"), web, activeId = "t1")
        val result = BrowserReducer.reduce(state, BrowserIntent.SwitchTab("t2"))
        assertEquals("t2", result.activeTabId)
        assertEquals("https://b.com", result.addressInput)
    }

    @Test
    fun `main frame error sets error flag`() {
        val state = stateWith(homeTab("t1"))
        val result = BrowserReducer.reduce(state, BrowserIntent.ReceivedError("t1", isForMainFrame = true))
        assertTrue(result.tabs.first().isError)
    }

    @Test
    fun `subframe error is ignored`() {
        val state = stateWith(homeTab("t1"))
        val result = BrowserReducer.reduce(state, BrowserIntent.ReceivedError("t1", isForMainFrame = false))
        assertFalse(result.tabs.first().isError)
    }
}
