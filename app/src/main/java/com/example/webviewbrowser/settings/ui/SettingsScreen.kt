package com.example.webviewbrowser.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.webviewbrowser.browser.SearchEngines
import com.example.webviewbrowser.data.prefs.model.CookiePolicy
import com.example.webviewbrowser.data.prefs.model.ThemeMode
import com.example.webviewbrowser.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    com.example.webviewbrowser.core.ui.SubScreenScaffold(title = "設定", onBack = onBack) { padding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("検索エンジン")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchEngines.ALL.forEach { engine ->
                Button(
                    onClick = { viewModel.setSearchEngine(engine.id) },
                    enabled = s.searchEngineId != engine.id,
                ) { Text(engine.label) }
            }
        }

        HorizontalDivider()
        SectionTitle("サイト設定（グローバル）")
        SwitchRow("JavaScript を有効化", s.javascriptEnabled, viewModel::setJavascriptEnabled)
        SwitchRow("ポップアップをブロック", s.blockPopups, viewModel::setBlockPopups)
        SwitchRow(
            "Cookie を受け入れる",
            s.cookiePolicy == CookiePolicy.ALL,
        ) { viewModel.setCookiePolicy(if (it) CookiePolicy.ALL else CookiePolicy.NONE) }
        SwitchRow("Do Not Track を送信", s.doNotTrack, viewModel::setDoNotTrack)

        HorizontalDivider()
        SectionTitle("外観")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                Button(onClick = { viewModel.setThemeMode(mode) }, enabled = s.themeMode != mode) {
                    Text(mode.name)
                }
            }
        }
        Text("文字サイズ: ${s.textScaling}%")
        Slider(
            value = s.textScaling.toFloat(),
            onValueChange = { viewModel.setTextScaling(it.toInt()) },
            valueRange = 50f..200f,
        )

        HorizontalDivider()
        SectionTitle("ダウンロード")
        Text("並列数: ${s.maxParallelDownloads}")
        Slider(
            value = s.maxParallelDownloads.toFloat(),
            onValueChange = { viewModel.setMaxParallelDownloads(it.toInt()) },
            valueRange = 1f..8f,
            steps = 6,
        )

        HorizontalDivider()
        SectionTitle("プライバシー")
        Button(onClick = { viewModel.clearCookiesAndCache() }, modifier = Modifier.fillMaxWidth()) {
            Text("Cookie とキャッシュを削除")
        }
        Button(onClick = { viewModel.clearHistory() }, modifier = Modifier.fillMaxWidth()) {
            Text("閲覧履歴を削除")
        }
    }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
