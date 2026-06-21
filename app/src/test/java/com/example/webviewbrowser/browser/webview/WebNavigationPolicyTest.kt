package com.example.webviewbrowser.browser.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebNavigationPolicyTest {

    @Test
    fun `http and web internal schemes stay in WebView`() {
        assertTrue(WebNavigationPolicy.canLoadInWebView("https://example.com/path"))
        assertTrue(WebNavigationPolicy.canLoadInWebView("http://example.com"))
        assertTrue(WebNavigationPolicy.canLoadInWebView("about:blank"))
        assertTrue(WebNavigationPolicy.canLoadInWebView("javascript:void(0)"))
    }

    @Test
    fun `app schemes are delegated outside WebView`() {
        assertFalse(WebNavigationPolicy.canLoadInWebView("mailto:user@example.com"))
        assertFalse(WebNavigationPolicy.canLoadInWebView("tel:+81123456789"))
        assertFalse(WebNavigationPolicy.canLoadInWebView("intent://scan/#Intent;scheme=zxing;end"))
    }
}
