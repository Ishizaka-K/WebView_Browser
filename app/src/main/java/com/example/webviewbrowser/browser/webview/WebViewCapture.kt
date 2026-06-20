package com.example.webviewbrowser.browser.webview

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * WebView のサムネイルを取得する。
 *
 * `webView.draw()` はハードウェアアクセラレーションされた WebView の内容（特にスクロール後）を
 * 取得できず白画像になるため、PixelCopy で実画面のピクセルを取得する。
 */
object WebViewCapture {

    private const val SCALE = 0.4f

    suspend fun capture(window: Window, webView: View): Bitmap? {
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return null

        val location = IntArray(2)
        webView.getLocationInWindow(location)
        val srcRect = Rect(location[0], location[1], location[0] + width, location[1] + height)
        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val full = suspendCancellableCoroutine<Bitmap?> { cont ->
            try {
                PixelCopy.request(
                    window,
                    srcRect,
                    source,
                    { result -> cont.resume(if (result == PixelCopy.SUCCESS) source else null) },
                    Handler(Looper.getMainLooper()),
                )
            } catch (e: IllegalArgumentException) {
                cont.resume(null)
            }
        } ?: return null

        val scaledW = (width * SCALE).toInt().coerceAtLeast(1)
        val scaledH = (height * SCALE).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(full, scaledW, scaledH, true)
    }
}
