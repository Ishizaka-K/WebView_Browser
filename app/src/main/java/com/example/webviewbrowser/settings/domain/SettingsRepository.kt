package com.example.webviewbrowser.settings.domain

import com.example.webviewbrowser.data.prefs.SettingsDataStore
import com.example.webviewbrowser.data.prefs.model.AppSettings
import com.example.webviewbrowser.data.prefs.model.CookiePolicy
import com.example.webviewbrowser.data.prefs.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 設定の読み書きをラップする。 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: SettingsDataStore,
) {
    val settings: Flow<AppSettings> = dataStore.settings

    suspend fun setSearchEngine(id: String) = dataStore.setSearchEngine(id)
    suspend fun setHomeUrl(url: String) = dataStore.setHomeUrl(url)
    suspend fun setJavascriptEnabled(enabled: Boolean) = dataStore.setJavascriptEnabled(enabled)
    suspend fun setBlockPopups(enabled: Boolean) = dataStore.setBlockPopups(enabled)
    suspend fun setCookiePolicy(policy: CookiePolicy) = dataStore.setCookiePolicy(policy)
    suspend fun setDoNotTrack(enabled: Boolean) = dataStore.setDoNotTrack(enabled)
    suspend fun setThemeMode(mode: ThemeMode) = dataStore.setThemeMode(mode)
    suspend fun setTextScaling(percent: Int) = dataStore.setTextScaling(percent)
    suspend fun setMaxParallelDownloads(count: Int) = dataStore.setMaxParallelDownloads(count)
}
