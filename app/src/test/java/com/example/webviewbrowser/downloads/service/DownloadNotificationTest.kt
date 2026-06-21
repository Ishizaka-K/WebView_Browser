package com.example.webviewbrowser.downloads.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.webviewbrowser.MainActivity
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.downloads.engine.DownloadProgress
import com.example.webviewbrowser.navigation.AppRoutes
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DownloadNotificationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `each completed download has an independent notification tag`() {
        val first = DownloadService.completionNotificationTag("download-a")
        val second = DownloadService.completionNotificationTag("download-b")

        assertNotEquals(first, second)
        assertTrue(first.startsWith("download:"))
    }

    @Test
    fun `completion notifications use a dedicated group`() {
        assertNotEquals(DownloadService.PROGRESS_CHANNEL_ID, DownloadService.COMPLETE_CHANNEL_ID)
        assertTrue(DownloadService.COMPLETION_GROUP.isNotBlank())
    }

    @Test
    fun `completed file notification is grouped and opens saved file`() {
        val notification = DownloadNotificationFactory.completion(context, completed("content://downloads/file-a"))
        val tapIntent = shadowOf(notification.contentIntent).savedIntent

        assertEquals(DownloadService.COMPLETION_GROUP, notification.group)
        assertNotNull(notification.contentIntent)
        assertEquals(Intent.ACTION_VIEW, tapIntent.action)
        assertEquals("content://downloads/file-a", tapIntent.dataString)
    }

    @Test
    fun `summary notification opens downloads screen and is marked summary`() {
        val notification = DownloadNotificationFactory.summary(context, 2)
        val tapIntent = shadowOf(notification.contentIntent).savedIntent

        assertTrue(notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
        assertEquals(DownloadService.COMPLETION_GROUP, notification.group)
        assertEquals(MainActivity::class.java.name, tapIntent.component?.className)
        assertEquals(AppRoutes.DOWNLOADS, tapIntent.getStringExtra(MainActivity.EXTRA_DESTINATION))
    }

    private fun completed(uri: String) = DownloadProgress(
        id = "download-a",
        fileName = "file-a.pdf",
        url = "https://example.com/file-a.pdf",
        mimeType = "application/pdf",
        totalBytes = 100,
        downloadedBytes = 100,
        status = DownloadStatus.COMPLETED,
        savedUri = uri,
        createdAt = 1,
        updatedAt = 2,
    )
}
