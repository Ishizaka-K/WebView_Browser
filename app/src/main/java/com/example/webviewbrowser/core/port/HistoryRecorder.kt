package com.example.webviewbrowser.core.port

/**
 * 履歴記録の最小 port。
 *
 * ブラウザコア(Task5) が main frame の正常完了時に呼ぶ。
 * 実装は History feature(Task8) が提供する。
 */
interface HistoryRecorder {
    /**
     * 訪問を記録する。同一 URL は visitCount を加算する。
     * 内部 URL / エラー / 失敗ナビゲーションは呼び出し側で除外する。
     */
    suspend fun record(url: String, title: String)
}
