package com.example.webviewbrowser.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlNormalizerTest {

    private val template = "https://www.google.com/search?q=%s"

    @Test
    fun `domain without scheme becomes https url`() {
        val result = UrlNormalizer.normalize("example.com", template)
        assertEquals(NormalizedTarget.Url("https://example.com"), result)
    }

    @Test
    fun `url with scheme is kept`() {
        val result = UrlNormalizer.normalize("https://a.b/c", template)
        assertEquals(NormalizedTarget.Url("https://a.b/c"), result)
    }

    @Test
    fun `plain words become search`() {
        val result = UrlNormalizer.normalize("hello world", template)
        assertTrue(result is NormalizedTarget.Search)
        result as NormalizedTarget.Search
        assertTrue(result.url.startsWith("https://www.google.com/search?q="))
        assertTrue(result.url.contains("hello"))
    }

    @Test
    fun `japanese query becomes search`() {
        val result = UrlNormalizer.normalize("天気 東京", template)
        assertTrue(result is NormalizedTarget.Search)
    }

    @Test
    fun `localhost with port is url`() {
        val result = UrlNormalizer.normalize("localhost:8080", template)
        assertEquals(NormalizedTarget.Url("https://localhost:8080"), result)
    }

    @Test
    fun `ipv4 with path is url`() {
        val result = UrlNormalizer.normalize("192.168.0.1/admin", template)
        assertEquals(NormalizedTarget.Url("https://192.168.0.1/admin"), result)
    }

    @Test
    fun `single word without dot is search`() {
        val result = UrlNormalizer.normalize("android", template)
        assertTrue(result is NormalizedTarget.Search)
    }

    @Test
    fun `non http schemes fall back to search`() {
        listOf("ftp://a.com/x", "file:///etc/passwd", "custom://do").forEach { input ->
            val result = UrlNormalizer.normalize(input, template)
            assertTrue("$input should be search", result is NormalizedTarget.Search)
        }
    }
}
