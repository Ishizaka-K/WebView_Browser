package com.example.webviewbrowser.downloads.engine

import com.example.webviewbrowser.data.db.entity.DownloadStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadTransitionPolicyTest {
    @Test
    fun `active download can pause or cancel`() {
        assertTrue(DownloadTransitionPolicy.canPause(DownloadStatus.RUNNING))
        assertTrue(DownloadTransitionPolicy.canCancel(DownloadStatus.QUEUED))
        assertFalse(DownloadTransitionPolicy.canRetry(DownloadStatus.RUNNING))
    }

    @Test
    fun `paused download can resume without retry`() {
        assertTrue(DownloadTransitionPolicy.canResume(DownloadStatus.PAUSED))
        assertTrue(DownloadTransitionPolicy.canCancel(DownloadStatus.PAUSED))
        assertFalse(DownloadTransitionPolicy.canRetry(DownloadStatus.PAUSED))
    }

    @Test
    fun `failed and canceled downloads can retry`() {
        assertTrue(DownloadTransitionPolicy.canRetry(DownloadStatus.FAILED))
        assertTrue(DownloadTransitionPolicy.canRetry(DownloadStatus.CANCELED))
        assertFalse(DownloadTransitionPolicy.canResume(DownloadStatus.FAILED))
    }

    @Test
    fun `completed download is terminal`() {
        assertFalse(DownloadTransitionPolicy.canPause(DownloadStatus.COMPLETED))
        assertFalse(DownloadTransitionPolicy.canResume(DownloadStatus.COMPLETED))
        assertFalse(DownloadTransitionPolicy.canRetry(DownloadStatus.COMPLETED))
        assertFalse(DownloadTransitionPolicy.canCancel(DownloadStatus.COMPLETED))
    }
}
