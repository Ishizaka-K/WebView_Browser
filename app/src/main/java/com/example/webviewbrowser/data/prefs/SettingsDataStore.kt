package com.example.webviewbrowser.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.webviewbrowser.data.prefs.model.AppSettings
import com.example.webviewbrowser.data.prefs.model.CookiePolicy
import com.example.webviewbrowser.data.prefs.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 設定値の永続化（Preferences DataStore）。
 * 設計の設定キー一覧を Flow で公開し、suspend で更新する。
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val HOME_URL = stringPreferencesKey("home_url")
        val JS_ENABLED = booleanPreferencesKey("javascript_enabled")
        val BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        val ACCEPT_COOKIES = stringPreferencesKey("accept_cookies")
        val DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
        val THEME = stringPreferencesKey("theme")
        val TEXT_SCALING = intPreferencesKey("text_scaling")
        val MAX_PARALLEL_DOWNLOADS = intPreferencesKey("max_parallel_downloads")
        val HOME_SHORTCUTS = stringPreferencesKey("home_shortcuts")
    }

    /**
     * ホームショートカット（titleurl を \n で連結）。
     * 未設定(null)は「既定を使う」を意味し、Flow では null を流す。
     */
    val homeShortcutsRaw: Flow<String?> = context.dataStore.data.map { it[Keys.HOME_SHORTCUTS] }

    suspend fun setHomeShortcutsRaw(raw: String) = edit { it[Keys.HOME_SHORTCUTS] = raw }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            searchEngineId = prefs[Keys.SEARCH_ENGINE] ?: AppSettings.DEFAULT.searchEngineId,
            homeUrl = prefs[Keys.HOME_URL] ?: AppSettings.DEFAULT.homeUrl,
            javascriptEnabled = prefs[Keys.JS_ENABLED] ?: AppSettings.DEFAULT.javascriptEnabled,
            blockPopups = prefs[Keys.BLOCK_POPUPS] ?: AppSettings.DEFAULT.blockPopups,
            cookiePolicy = prefs[Keys.ACCEPT_COOKIES]?.let { runCatching { CookiePolicy.valueOf(it) }.getOrNull() }
                ?: AppSettings.DEFAULT.cookiePolicy,
            doNotTrack = prefs[Keys.DO_NOT_TRACK] ?: AppSettings.DEFAULT.doNotTrack,
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: AppSettings.DEFAULT.themeMode,
            textScaling = (prefs[Keys.TEXT_SCALING] ?: AppSettings.DEFAULT.textScaling)
                .coerceIn(MIN_TEXT_SCALING, MAX_TEXT_SCALING),
            maxParallelDownloads = (prefs[Keys.MAX_PARALLEL_DOWNLOADS] ?: AppSettings.DEFAULT.maxParallelDownloads)
                .coerceIn(1, MAX_PARALLEL_DOWNLOADS),
        )
    }

    suspend fun setSearchEngine(id: String) = edit { it[Keys.SEARCH_ENGINE] = id }
    suspend fun setHomeUrl(url: String) = edit { it[Keys.HOME_URL] = url }
    suspend fun setJavascriptEnabled(enabled: Boolean) = edit { it[Keys.JS_ENABLED] = enabled }
    suspend fun setBlockPopups(enabled: Boolean) = edit { it[Keys.BLOCK_POPUPS] = enabled }
    suspend fun setCookiePolicy(policy: CookiePolicy) = edit { it[Keys.ACCEPT_COOKIES] = policy.name }
    suspend fun setDoNotTrack(enabled: Boolean) = edit { it[Keys.DO_NOT_TRACK] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }

    /** 文字サイズは 50〜200% に丸める。 */
    suspend fun setTextScaling(percent: Int) =
        edit { it[Keys.TEXT_SCALING] = percent.coerceIn(MIN_TEXT_SCALING, MAX_TEXT_SCALING) }

    /** 並列数は 1〜MAX に丸める。 */
    suspend fun setMaxParallelDownloads(count: Int) =
        edit { it[Keys.MAX_PARALLEL_DOWNLOADS] = count.coerceIn(1, MAX_PARALLEL_DOWNLOADS) }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    companion object {
        const val MIN_TEXT_SCALING = 50
        const val MAX_TEXT_SCALING = 200
        const val MAX_PARALLEL_DOWNLOADS = 8
    }
}
