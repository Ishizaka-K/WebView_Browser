package com.example.webviewbrowser.downloads.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadPresentationTest {
    @Test
    fun `known total formats current and total size`() {
        assertEquals("1.0 MB / 2.0 MB", formatDownloadSize(1024 * 1024, 2L * 1024 * 1024))
    }

    @Test
    fun `unknown total formats downloaded size only`() {
        assertEquals("512 B", formatDownloadSize(512, 0))
    }
}
