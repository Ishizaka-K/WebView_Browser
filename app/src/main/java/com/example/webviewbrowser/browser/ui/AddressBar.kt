package com.example.webviewbrowser.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Chrome 風オムニボックス。角丸ピル内に 情報アイコン + 入力 + クリア/リロード を収める。
 */
@Composable
fun AddressBar(
    text: String,
    isLoading: Boolean,
    progress: Int,
    onTextChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onReloadOrStop: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    trailingMenu: @Composable () -> Unit = {},
) {
    val isSecure = text.startsWith("https://")
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                        .padding(start = 12.dp, end = 4.dp),
                ) {
                Icon(
                    imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    if (text.isEmpty()) {
                        Text(
                            "検索またはURLを入力",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onSubmit(text) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                    )
                }
                if (text.isNotEmpty()) {
                    IconButton(onClick = { onTextChange("") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "クリア", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onReloadOrStop, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isLoading) Icons.Default.Clear else Icons.Default.Refresh,
                        contentDescription = if (isLoading) "停止" else "再読み込み",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            trailingMenu()
        }
            if (isLoading && progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                )
            }
        }
    }
}
