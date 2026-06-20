package com.example.webviewbrowser.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webviewbrowser.data.prefs.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: SettingsDataStore,
) : ViewModel() {

    val shortcuts: StateFlow<List<HomeShortcut>> = settings.homeShortcutsRaw
        .map { raw -> raw?.let(::decode) ?: HomeShortcut.DEFAULTS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeShortcut.DEFAULTS)

    fun addShortcut(title: String, url: String) {
        val normalizedUrl = if (url.startsWith("http")) url else "https://$url"
        val name = title.ifBlank { hostOf(normalizedUrl) }
        viewModelScope.launch {
            val current = settings.homeShortcutsRaw.first()?.let(::decode) ?: HomeShortcut.DEFAULTS
            if (current.any { it.url == normalizedUrl }) return@launch
            settings.setHomeShortcutsRaw(encode(current + HomeShortcut(name, normalizedUrl)))
        }
    }

    fun removeShortcut(url: String) {
        viewModelScope.launch {
            val current = settings.homeShortcutsRaw.first()?.let(::decode) ?: HomeShortcut.DEFAULTS
            settings.setHomeShortcutsRaw(encode(current.filterNot { it.url == url }))
        }
    }

    private fun encode(list: List<HomeShortcut>): String =
        list.joinToString(ENTRY_SEP) { it.title + FIELD_SEP + it.url }

    private fun decode(raw: String): List<HomeShortcut> =
        if (raw.isBlank()) {
            emptyList()
        } else {
            raw.split(ENTRY_SEP).mapNotNull { line ->
                val parts = line.split(FIELD_SEP)
                if (parts.size == 2 && parts[1].isNotBlank()) HomeShortcut(parts[0], parts[1]) else null
            }
        }

    private fun hostOf(url: String): String =
        url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")

    private companion object {
        const val FIELD_SEP = "\t"
        const val ENTRY_SEP = "\n"
    }
}
