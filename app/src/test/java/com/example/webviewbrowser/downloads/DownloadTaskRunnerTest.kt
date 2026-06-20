package com.example.webviewbrowser.downloads

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.webviewbrowser.data.db.AppDatabase
import com.example.webviewbrowser.data.db.entity.DestinationType
import com.example.webviewbrowser.data.db.entity.DownloadEntity
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.downloads.data.DownloadRepository
import com.example.webviewbrowser.downloads.engine.DownloadTaskRunner
import com.example.webviewbrowser.downloads.engine.FileStore
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DownloadTaskRunnerTest {

    private lateinit var db: AppDatabase
    private lateinit var server: MockWebServer
    private lateinit var repository: DownloadRepository
    private lateinit var runner: DownloadTaskRunner
    private lateinit var fileStore: TestFileStore

    /** publish で MediaStore を触らないテスト用 FileStore。 */
    private class TestFileStore(context: Context) : FileStore(context) {
        var publishedFrom: ByteArray? = null
        override suspend fun publish(id: String, fileName: String, mimeType: String?): String {
            publishedFrom = partFile(id).readBytes()
            return "file://done/$fileName"
        }
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = DownloadRepository(db.downloadDao())
        fileStore = TestFileStore(context)
        runner = DownloadTaskRunner(OkHttpClient(), repository, fileStore)
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    private fun entity(id: String, total: Long, downloaded: Long) = DownloadEntity(
        id = id, url = server.url("/file").toString(), fileName = "f.bin", mimeType = "application/octet-stream",
        totalBytes = total, downloadedBytes = downloaded, status = DownloadStatus.QUEUED,
        etag = "\"abc\"", lastModified = null, tempPath = fileStore.partFile(id).absolutePath,
        destinationType = DestinationType.APP_PRIVATE, relativePath = null, savedUri = null,
        sourcePageUrl = null, userAgent = "UA", referer = null, createdAt = 0, updatedAt = 0,
    )

    @Test
    fun `full download via 200 completes`() = runTest {
        val data = ByteArray(1000) { (it % 256).toByte() }
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(data)))
        repository.upsert(entity("d1", total = 1000, downloaded = 0))

        val result = runner.run("d1", cookieHeader = null)

        assertEquals(DownloadTaskRunner.Result.COMPLETED, result)
        assertArrayEquals(data, fileStore.publishedFrom)
        assertEquals(DownloadStatus.COMPLETED, repository.get("d1")?.status)
    }

    @Test
    fun `resume via 206 appends remaining bytes`() = runTest {
        val full = ByteArray(1000) { (it % 256).toByte() }
        val firstHalf = full.copyOfRange(0, 400)
        val secondHalf = full.copyOfRange(400, 1000)
        // 既存 .part に前半を書いておく。
        File(fileStore.partFile("d2").absolutePath).apply {
            parentFile?.mkdirs()
            writeBytes(firstHalf)
        }
        server.enqueue(
            MockResponse().setResponseCode(206)
                .addHeader("Content-Range", "bytes 400-999/1000")
                .setBody(Buffer().write(secondHalf)),
        )
        repository.upsert(entity("d2", total = 1000, downloaded = 400))

        val result = runner.run("d2", cookieHeader = null)

        assertEquals(DownloadTaskRunner.Result.COMPLETED, result)
        assertArrayEquals(full, fileStore.publishedFrom)
        // 送られた Range ヘッダを検証。
        val recorded = server.takeRequest()
        assertEquals("bytes=400-", recorded.getHeader("Range"))
        assertEquals("identity", recorded.getHeader("Accept-Encoding"))
    }

    @Test
    fun `content range mismatch fails`() = runTest {
        File(fileStore.partFile("d3").absolutePath).apply { parentFile?.mkdirs(); writeBytes(ByteArray(400)) }
        server.enqueue(
            MockResponse().setResponseCode(206)
                .addHeader("Content-Range", "bytes 999-999/1000") // 開始位置が不一致
                .setBody(Buffer().write(ByteArray(1))),
        )
        repository.upsert(entity("d3", total = 1000, downloaded = 400))

        val result = runner.run("d3", cookieHeader = null)

        assertEquals(DownloadTaskRunner.Result.FAILED, result)
        assertEquals(DownloadStatus.FAILED, repository.get("d3")?.status)
    }
}
