package com.example.webviewbrowser.downloads.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.webviewbrowser.R
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.data.prefs.SettingsDataStore
import com.example.webviewbrowser.downloads.data.DownloadRepository
import com.example.webviewbrowser.downloads.engine.DownloadProgress
import com.example.webviewbrowser.downloads.engine.DownloadTaskRunner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * ダウンロード実行の所有者（Foreground Service）。
 *
 * - `SupervisorJob + Dispatchers.IO` で実行コルーチンを所有する。
 * - DB の QUEUED を並列上限まで drain して実行する。
 * - RUNNING/QUEUED がある間だけ前面で動作し、0 になったら停止する。
 * - pause/cancel はアクション Intent で受け、対象ジョブを停止する。
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var repository: DownloadRepository
    @Inject lateinit var runner: DownloadTaskRunner
    @Inject lateinit var settings: SettingsDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val scheduleMutex = Mutex()
    private var observeJob: Job? = null
    private var started = false
    private var seededCompletedNotifications = false
    private val knownCompletedIds = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForegroundCompat(PROGRESS_NOTIFICATION_ID, buildNotification(emptyList()))
        observeJob = repository.observeAll()
            .onEach { list ->
                publishNewCompletionNotifications(list)
                val active = list.count {
                    it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED
                }
                if (active > 0) started = true
                if (active <= 0 && started) {
                    stopSelfSafely()
                } else {
                    notificationManager().notify(PROGRESS_NOTIFICATION_ID, buildNotification(list))
                    schedule()
                }
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        started = true
        when (intent?.action) {
            ACTION_PAUSE -> intent.idExtra()?.let { pause(it) }
            ACTION_CANCEL -> intent.idExtra()?.let { cancel(it) }
            else -> scope.launch {
                schedule()
                stopIfIdle()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    /** QUEUED を並列上限まで起動する。 */
    private fun schedule() {
        scope.launch {
            scheduleMutex.withLock {
                val limit = settings.settings.first().maxParallelDownloads.coerceAtLeast(1)
                val queued = repository.getByStatuses(listOf(DownloadStatus.QUEUED))
                for (entity in queued) {
                    if (jobs.size >= limit) break
                    if (jobs.containsKey(entity.id)) continue
                    launchJob(entity.id)
                }
            }
        }
    }

    private fun launchJob(id: String) {
        val job = scope.launch {
            try {
                // Cookie は runner が CookieManager から実行時取得する。
                runner.run(id, cookieHeader = null)
            } finally {
                jobs.remove(id)
                schedule()
            }
        }
        jobs[id] = job
    }

    private fun pause(id: String) {
        scope.launch {
            jobs.remove(id)?.cancelAndJoin()
            if (repository.get(id)?.status != DownloadStatus.COMPLETED) {
                repository.updateStatus(id, DownloadStatus.PAUSED)
            }
            stopIfIdle()
        }
    }

    private fun cancel(id: String) {
        scope.launch {
            val entity = repository.get(id)
            jobs.remove(id)?.cancelAndJoin()
            if (repository.get(id)?.status != DownloadStatus.COMPLETED) {
                repository.updateStatus(id, DownloadStatus.CANCELED)
            }
            entity?.tempPath?.let { runCatching { File(it).delete() } }
            stopIfIdle()
        }
    }

    private suspend fun stopIfIdle() {
        val active = repository.getByStatuses(listOf(DownloadStatus.RUNNING, DownloadStatus.QUEUED))
        if (active.isEmpty()) stopSelfSafely()
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(list: List<DownloadProgress>): Notification {
        val running = list.filter { it.status == DownloadStatus.RUNNING }
        val builder = NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(DownloadNotificationFactory.downloadsPendingIntent(this, PROGRESS_NOTIFICATION_ID))
        val first = running.firstOrNull()
        if (first != null && first.totalBytes > 0) {
            val activeCount = list.count {
                it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED
            }
            builder.setContentTitle(if (activeCount > 1) "ダウンロード中 • ${activeCount}件" else "ダウンロード中")
                .setContentText("${first.fileName} (${first.percent}%)")
                .setProgress(100, first.percent, false)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "一時停止",
                    servicePendingIntent(ACTION_PAUSE, first.id, 1),
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "キャンセル",
                    servicePendingIntent(ACTION_CANCEL, first.id, 2),
                )
            if (activeCount > 1) {
                builder.setStyle(
                    NotificationCompat.InboxStyle().also { style ->
                        list.filter {
                            it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED
                        }.take(5).forEach { item ->
                            style.addLine("${item.fileName}  ${item.percent}%")
                        }
                    },
                )
            }
        } else {
            val active = list.count { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED }
            builder.setContentText(if (active > 0) "ダウンロード中: ${active}件" else "ダウンロード準備中")
                .setProgress(0, 0, active > 0)
        }
        return builder.build()
    }

    private fun publishNewCompletionNotifications(list: List<DownloadProgress>) {
        val completed = list.filter { it.status == DownloadStatus.COMPLETED }
        if (!seededCompletedNotifications) {
            knownCompletedIds += completed.map { it.id }
            seededCompletedNotifications = true
            return
        }
        val newlyCompleted = completed.filterNot { it.id in knownCompletedIds }
        newlyCompleted.forEach { download ->
            knownCompletedIds += download.id
            notificationManager().notify(
                completionNotificationTag(download.id),
                COMPLETION_NOTIFICATION_ID,
                DownloadNotificationFactory.completion(this, download),
            )
        }
        if (newlyCompleted.isNotEmpty()) {
            notificationManager().notify(
                GROUP_SUMMARY_NOTIFICATION_ID,
                DownloadNotificationFactory.summary(this, completed.size),
            )
        }
    }

    private fun servicePendingIntent(action: String, id: String, actionCode: Int): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getService(
            this,
            completionRequestCode(id) + actionCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(id, notification)
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager().createNotificationChannels(
                listOf(
                    NotificationChannel(
                        PROGRESS_CHANNEL_ID,
                        "ダウンロードの進捗",
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                    NotificationChannel(
                        COMPLETE_CHANNEL_ID,
                        "ダウンロード完了",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                ),
            )
        }
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    private fun Intent.idExtra(): String? = getStringExtra(EXTRA_ID)

    companion object {
        internal const val PROGRESS_CHANNEL_ID = "download_progress"
        internal const val COMPLETE_CHANNEL_ID = "download_complete"
        internal const val COMPLETION_GROUP = "completed_downloads"
        private const val PROGRESS_NOTIFICATION_ID = 1001
        private const val GROUP_SUMMARY_NOTIFICATION_ID = 1002
        private const val COMPLETION_NOTIFICATION_ID = 1003
        private const val EXTRA_ID = "download_id"
        const val ACTION_START = "start"
        const val ACTION_PAUSE = "pause"
        const val ACTION_CANCEL = "cancel"

        internal fun completionNotificationTag(id: String): String = "download:$id"

        internal fun completionRequestCode(id: String): Int =
            10_000 + (id.hashCode() and 0x3fffffff)

        fun start(context: Context, action: String, id: String? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                this.action = action
                id?.let { putExtra(EXTRA_ID, it) }
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
