package com.example.webviewbrowser.downloads.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.webviewbrowser.core.ui.EmptyState
import com.example.webviewbrowser.core.ui.SubScreenScaffold
import com.example.webviewbrowser.core.ui.appSurface
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.downloads.DownloadsViewModel
import com.example.webviewbrowser.downloads.engine.DownloadProgress
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SubScreenScaffold(title = "ダウンロード", onBack = onBack) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (downloads.isEmpty()) {
                EmptyState("ダウンロードはありません")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight().widthIn(max = 960.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { NotificationPermissionCard() }
                    items(downloads, key = { it.id }) { download ->
                        DownloadCard(
                            download = download,
                            onPause = { viewModel.pause(download.id) },
                            onResume = { viewModel.resume(download.id) },
                            onCancel = { viewModel.cancel(download.id) },
                            onRetry = { viewModel.retry(download.id) },
                            onOpen = {
                                download.savedUri?.let { uri ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(uri), download.mimeType ?: "*/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        })
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionCard() {
    val context = LocalContext.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled() }

    if (notificationsEnabled) return
    Row(
        modifier = Modifier.fillMaxWidth().appSurface(RoundedCornerShape(20.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Column(Modifier.weight(1f)) {
            Text("通知がオフです", fontWeight = FontWeight.SemiBold)
            Text(
                "ダウンロードは継続しますが、完了のお知らせは表示されません。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = {
            val canRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (canRequest && !requestedOnce) {
                requestedOnce = true
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                })
            }
        }) { Text("有効化") }
    }
}

@Composable
private fun DownloadCard(
    download: DownloadProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    val presentation = downloadStatusPresentation(download.status)
    Row(
        modifier = Modifier.fillMaxWidth().appSurface().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = presentation.color.copy(alpha = 0.15f),
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = null,
                tint = presentation.color,
                modifier = Modifier.padding(12.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                download.fileName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${presentation.label}  •  ${formatDownloadSize(download.downloadedBytes, download.totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(download.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (download.status == DownloadStatus.RUNNING || download.status == DownloadStatus.QUEUED) {
                if (download.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { download.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        DownloadActions(download.status, onPause, onResume, onCancel, onRetry, onOpen)
    }
}

@Composable
private fun DownloadActions(
    status: DownloadStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    Row {
        when (status) {
            DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                IconButton(onClick = onPause) { Icon(Icons.Default.Pause, contentDescription = "一時停止") }
                IconButton(onClick = onCancel) { Icon(Icons.Default.Cancel, contentDescription = "キャンセル") }
            }
            DownloadStatus.PAUSED -> {
                IconButton(onClick = onResume) { Icon(Icons.Default.PlayArrow, contentDescription = "再開") }
                IconButton(onClick = onCancel) { Icon(Icons.Default.Cancel, contentDescription = "キャンセル") }
            }
            DownloadStatus.FAILED, DownloadStatus.CANCELED ->
                IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, contentDescription = "再試行") }
            DownloadStatus.COMPLETED ->
                IconButton(onClick = onOpen) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "開く")
                }
        }
    }
}

internal data class DownloadStatusPresentation(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
)

@Composable
internal fun downloadStatusPresentation(status: DownloadStatus): DownloadStatusPresentation = when (status) {
    DownloadStatus.QUEUED -> DownloadStatusPresentation("待機中", Icons.Default.Description, MaterialTheme.colorScheme.primary)
    DownloadStatus.RUNNING -> DownloadStatusPresentation("ダウンロード中", Icons.Default.Description, MaterialTheme.colorScheme.primary)
    DownloadStatus.PAUSED -> DownloadStatusPresentation("一時停止", Icons.Default.Pause, MaterialTheme.colorScheme.tertiary)
    DownloadStatus.COMPLETED -> DownloadStatusPresentation("完了", Icons.Default.CheckCircle, Color(0xFF188038))
    DownloadStatus.FAILED -> DownloadStatusPresentation("失敗", Icons.Default.Error, MaterialTheme.colorScheme.error)
    DownloadStatus.CANCELED -> DownloadStatusPresentation("キャンセル済み", Icons.Default.Cancel, MaterialTheme.colorScheme.onSurfaceVariant)
}

internal fun formatDownloadSize(downloaded: Long, total: Long): String {
    val current = formatBytes(downloaded)
    return if (total > 0) "$current / ${formatBytes(total)}" else current
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}
