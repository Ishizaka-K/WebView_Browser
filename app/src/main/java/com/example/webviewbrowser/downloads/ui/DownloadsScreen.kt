package com.example.webviewbrowser.downloads.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.webviewbrowser.core.ui.EmptyState
import com.example.webviewbrowser.core.ui.SubScreenScaffold
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.downloads.DownloadsViewModel
import com.example.webviewbrowser.downloads.engine.DownloadProgress

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SubScreenScaffold(title = "ダウンロード", onBack = onBack) { padding ->
        if (downloads.isEmpty()) {
            EmptyState("ダウンロードはありません", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp)) {
                items(downloads, key = { it.id }) { d ->
                    DownloadRow(
                        d = d,
                        onPause = { viewModel.pause(d.id) },
                        onResume = { viewModel.resume(d.id) },
                        onCancel = { viewModel.cancel(d.id) },
                        onRetry = { viewModel.retry(d.id) },
                        onOpen = {
                            d.savedUri?.let { uri ->
                                runCatching {
                                    val view = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(uri), d.mimeType ?: "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(view)
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    d: DownloadProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(d.fileName, maxLines = 1)
        Text("${d.status} ・ ${d.percent}%")
        if (d.status == DownloadStatus.RUNNING || d.status == DownloadStatus.QUEUED) {
            LinearProgressIndicator(progress = { d.percent / 100f }, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (d.status) {
                DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                    Button(onClick = onPause) { Text("一時停止") }
                    Button(onClick = onCancel) { Text("キャンセル") }
                }
                DownloadStatus.PAUSED -> {
                    Button(onClick = onResume) { Text("再開") }
                    Button(onClick = onCancel) { Text("キャンセル") }
                }
                DownloadStatus.FAILED -> {
                    Button(onClick = onRetry) { Text("再試行") }
                }
                DownloadStatus.COMPLETED -> {
                    Button(onClick = onOpen) { Text("開く") }
                }
                DownloadStatus.CANCELED -> {
                    Button(onClick = onRetry) { Text("再試行") }
                }
            }
        }
    }
}
