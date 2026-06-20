package com.example.webviewbrowser.downloads

import com.example.webviewbrowser.downloads.engine.DownloadEngine
import com.example.webviewbrowser.downloads.engine.DownloadProgress
import com.example.webviewbrowser.downloads.engine.DownloadRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** ダウンロード操作の入口。UI/統合層から使う。 */
class DownloadUseCase @Inject constructor(
    private val engine: DownloadEngine,
) {
    suspend fun enqueue(request: DownloadRequest): String = engine.enqueue(request)
    suspend fun pause(id: String) = engine.pause(id)
    suspend fun resume(id: String) = engine.resume(id)
    suspend fun cancel(id: String) = engine.cancel(id)
    suspend fun retry(id: String) = engine.retry(id)
    fun observeAll(): Flow<List<DownloadProgress>> = engine.observeAll()
}
