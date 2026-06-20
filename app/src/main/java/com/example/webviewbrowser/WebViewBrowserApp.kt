package com.example.webviewbrowser

import android.app.Application
import com.example.webviewbrowser.downloads.data.DownloadRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * アプリケーションエントリポイント。Hilt のルートコンポーネントを起動する。
 */
@HiltAndroidApp
class WebViewBrowserApp : Application() {

    @Inject
    lateinit var downloadRepository: DownloadRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // プロセス再起動後、DB に残った RUNNING を PAUSED に正規化する。
        // 自動再開はせず、ユーザー操作の resume/retry を待つ。
        appScope.launch { downloadRepository.normalizeRunningToPaused() }
    }
}
