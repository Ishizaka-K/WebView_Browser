package com.example.webviewbrowser.downloads.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.webviewbrowser.MainActivity
import com.example.webviewbrowser.downloads.engine.DownloadProgress
import com.example.webviewbrowser.navigation.AppRoutes

/** 完了通知をServiceのライフサイクルから分離し、実体を検証可能にする。 */
internal object DownloadNotificationFactory {
    fun completion(context: Context, download: DownloadProgress): Notification {
        val contentIntent = download.savedUri?.let { savedUri ->
            val open = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(savedUri.toUri(), download.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            PendingIntent.getActivity(
                context,
                DownloadService.completionRequestCode(download.id),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } ?: downloadsPendingIntent(context, DownloadService.completionRequestCode(download.id))

        return NotificationCompat.Builder(context, DownloadService.COMPLETE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("ダウンロード完了")
            .setContentText(download.fileName)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setGroup(DownloadService.COMPLETION_GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .build()
    }

    fun summary(context: Context, completedCount: Int): Notification =
        NotificationCompat.Builder(context, DownloadService.COMPLETE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("ダウンロード")
            .setContentText("完了したファイル: ${completedCount}件")
            .setContentIntent(downloadsPendingIntent(context, GROUP_SUMMARY_REQUEST_CODE))
            .setAutoCancel(true)
            .setGroup(DownloadService.COMPLETION_GROUP)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .build()

    fun downloadsPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DESTINATION, AppRoutes.DOWNLOADS)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val GROUP_SUMMARY_REQUEST_CODE = 1002
}
