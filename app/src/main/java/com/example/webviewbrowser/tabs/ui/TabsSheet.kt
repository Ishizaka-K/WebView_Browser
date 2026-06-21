package com.example.webviewbrowser.tabs.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.webviewbrowser.browser.TabUiState

/**
 * Chrome 風タブグリッド。2 カラムで サムネイル + ファビコン + タイトル + 閉じる を表示する。
 */
@Composable
fun TabsSheet(
    tabs: List<TabUiState>,
    activeTabId: String?,
    thumbnailProvider: (String) -> ImageBitmap?,
    faviconProvider: (String) -> ImageBitmap?,
    onSwitch: (String) -> Unit,
    onClose: (String) -> Unit,
    onMoveTab: (fromIndex: Int, toIndex: Int) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    var draggedTabId by remember { mutableStateOf<String?>(null) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("タブ (${tabs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNewTab) { Icon(Icons.Default.Add, contentDescription = "新しいタブ") }
                    TextButton(onClick = onDismiss) { Text("完了") }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp),
            ) {
                itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                    TabCard(
                        tab = tab,
                        isActive = tab.id == activeTabId,
                        isDragging = tab.id == draggedTabId,
                        thumbnail = thumbnailProvider(tab.id),
                        favicon = faviconProvider(tab.id),
                        onClick = { onSwitch(tab.id); onDismiss() },
                        onClose = { onClose(tab.id) },
                        modifier = Modifier.pointerInput(tab.id, index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    // LazyGrid 内の座標でドラッグ位置を追跡し、移動先カードを判定する。
                                    val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                    if (item != null) {
                                        draggedTabId = tab.id
                                        draggedIndex = index
                                        dragPosition = Offset(
                                            x = item.offset.x + offset.x,
                                            y = item.offset.y + offset.y,
                                        )
                                    }
                                },
                                onDragEnd = {
                                    draggedTabId = null
                                    draggedIndex = -1
                                    dragPosition = null
                                },
                                onDragCancel = {
                                    draggedTabId = null
                                    draggedIndex = -1
                                    dragPosition = null
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentPosition = dragPosition
                                    if (currentPosition != null) {
                                        val nextPosition = currentPosition + dragAmount
                                        dragPosition = nextPosition
                                        val targetIndex = gridState.itemIndexAt(nextPosition)
                                        if (targetIndex != null && draggedIndex >= 0 && targetIndex != draggedIndex) {
                                            onMoveTab(draggedIndex, targetIndex)
                                            draggedIndex = targetIndex
                                        }
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: TabUiState,
    isActive: Boolean,
    isDragging: Boolean,
    thumbnail: ImageBitmap?,
    favicon: ImageBitmap?,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier
            .alpha(if (isDragging) 0.65f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .border(if (isActive) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FaviconView(favicon = favicon)
            Text(
                text = tab.title.ifBlank { if (tab.isHome) "ホーム" else tab.url },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "タブを閉じる", modifier = Modifier.size(16.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                )
            } else {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun LazyGridState.itemIndexAt(position: Offset): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull { item ->
        position.x >= item.offset.x &&
            position.x <= item.offset.x + item.size.width &&
            position.y >= item.offset.y &&
            position.y <= item.offset.y + item.size.height
    }?.index

@Composable
private fun FaviconView(favicon: ImageBitmap?) {
    Box(
        modifier = Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (favicon != null) {
            Image(bitmap = favicon, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                Icons.Default.Public,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
