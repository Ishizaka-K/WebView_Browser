package com.example.webviewbrowser.data.db.entity

/** ダウンロードの状態。 */
enum class DownloadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED,
}

/** タブの表示種別。 */
enum class TabContentType {
    WEB,
    HOME,
}

/** ダウンロード保存先の種別。 */
enum class DestinationType {
    /** アプリ専用領域（一時 .part）。 */
    APP_PRIVATE,

    /** 公開 MediaStore Downloads。 */
    MEDIA_STORE_DOWNLOADS,
}
