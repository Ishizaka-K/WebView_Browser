package com.example.webviewbrowser.browser.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserLayoutPolicyTest {
    @Test
    fun `compact and split widths use phone layout`() {
        assertEquals(BrowserLayoutMode.PHONE, BrowserLayoutPolicy.forWidthDp(360))
        assertEquals(BrowserLayoutMode.PHONE, BrowserLayoutPolicy.forWidthDp(599))
    }

    @Test
    fun `tablet width uses tab strip layout`() {
        assertEquals(BrowserLayoutMode.TABLET, BrowserLayoutPolicy.forWidthDp(600))
        assertEquals(BrowserLayoutMode.TABLET, BrowserLayoutPolicy.forWidthDp(1280))
    }
}
