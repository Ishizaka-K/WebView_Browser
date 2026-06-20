package com.example.webviewbrowser.settings

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webviewbrowser.data.prefs.model.AppSettings
import com.example.webviewbrowser.data.prefs.model.CookiePolicy
import com.example.webviewbrowser.data.prefs.model.ThemeMode
import com.example.webviewbrowser.history.domain.HistoryUseCase
import com.example.webviewbrowser.settings.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SettingsRepository,
    private val historyUseCase: HistoryUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.DEFAULT)

    fun setSearchEngine(id: String) = launch { repository.setSearchEngine(id) }
    fun setJavascriptEnabled(enabled: Boolean) = launch { repository.setJavascriptEnabled(enabled) }
    fun setBlockPopups(enabled: Boolean) = launch { repository.setBlockPopups(enabled) }
    fun setCookiePolicy(policy: CookiePolicy) = launch { repository.setCookiePolicy(policy) }
    fun setDoNotTrack(enabled: Boolean) = launch { repository.setDoNotTrack(enabled) }
    fun setThemeMode(mode: ThemeMode) = launch { repository.setThemeMode(mode) }
    fun setTextScaling(percent: Int) = launch { repository.setTextScaling(percent) }
    fun setMaxParallelDownloads(count: Int) = launch { repository.setMaxParallelDownloads(count) }

    /** Cookie とキャッシュを削除する（履歴削除は統合層が配線）。 */
    fun clearCookiesAndCache() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
            }
            withContext(Dispatchers.IO) {
                context.cacheDir.deleteRecursively()
            }
        }
    }

    /** 閲覧履歴を削除する。 */
    fun clearHistory() = launch { historyUseCase.clearAll() }

    private inline fun launch(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
