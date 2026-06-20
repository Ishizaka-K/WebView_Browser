package com.example.webviewbrowser.downloads.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat(NOTIFICATION_ID, buildNotification(emptyList()))
        observeJob = repository.observeAll()
            .onEach { list ->
                val active = list.count {
                    it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED
                }
                if (active > 0) started = true
                if (active <= 0 && started) {
                    stopSelfSafely()
                } else {
                    notificationManager().notify(NOTIFICATION_ID, buildNotification(list))
                    schedule()
                }
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> intent.idExtra()?.let { pause(it) }
            ACTION_CANCEL -> intent.idExtra()?.let { cancel(it) }
            else -> scope.launch { schedule() }
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
        }
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(list: List<DownloadProgress>): Notification {
        val running = list.filter { it.status == DownloadStatus.RUNNING }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        val first = running.firstOrNull()
        if (first != null && first.totalBytes > 0) {
            builder.setContentText("${first.fileName} (${first.percent}%)")
                .setProgress(100, first.percent, false)
        } else {
            val active = list.count { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED }
            builder.setContentText(if (active > 0) "ダウンロード中: ${active}件" else "ダウンロード準備中")
                .setProgress(0, 0, active > 0)
        }
        return builder.build()
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(id, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager().createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "ダウンロード", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    private fun Intent.idExtra(): String? = getStringExtra(EXTRA_ID)

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_ID = "download_id"
        const val ACTION_START = "start"
        const val ACTION_PAUSE = "pause"
        const val ACTION_CANCEL = "cancel"

        fun start(context: Context, action: String, id: String? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                this.action = action
                id?.let { putExtra(EXTRA_ID, it) }
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
