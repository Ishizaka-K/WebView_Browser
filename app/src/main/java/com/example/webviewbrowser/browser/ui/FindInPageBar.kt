package com.example.webviewbrowser.browser.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.webviewbrowser.browser.FindState

@Composable
fun FindInPageBar(
    state: FindState,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                modifier = Modifier.weight(1f),
                placeholder = { Text("ページ内を検索") },
            )
            val label = if (state.matchCount > 0) "${state.activeIndex + 1}/${state.matchCount}" else "0/0"
            Text(label, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = onPrev) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "前へ") }
            IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "次へ") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "閉じる") }
        }
    }
}
