package com.example.webviewbrowser.data.db

import androidx.room.TypeConverter
import com.example.webviewbrowser.data.db.entity.DestinationType
import com.example.webviewbrowser.data.db.entity.DownloadStatus
import com.example.webviewbrowser.data.db.entity.TabContentType

/** Enum を文字列として保存するためのコンバータ。 */
class Converters {
    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)

    @TypeConverter
    fun fromTabContentType(value: TabContentType): String = value.name

    @TypeConverter
    fun toTabContentType(value: String): TabContentType = TabContentType.valueOf(value)

    @TypeConverter
    fun fromDestinationType(value: DestinationType): String = value.name

    @TypeConverter
    fun toDestinationType(value: String): DestinationType = DestinationType.valueOf(value)
}
