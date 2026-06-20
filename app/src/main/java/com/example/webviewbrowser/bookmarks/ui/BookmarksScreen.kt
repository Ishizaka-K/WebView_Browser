package com.example.webviewbrowser.bookmarks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.webviewbrowser.bookmarks.BookmarksViewModel
import com.example.webviewbrowser.data.db.entity.BookmarkEntity
import com.example.webviewbrowser.data.db.entity.BookmarkFolderEntity

@Composable
fun BookmarksScreen(
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var draggedBookmarkId by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    val folderBounds = remember { mutableStateMapOf<String, Rect>() }
    val hoveredFolderId = dragPosition?.let { position ->
        folderBounds.entries.firstOrNull { (_, bounds) -> bounds.contains(position) }?.key
    }

    LaunchedEffect(state.folders) {
        val visibleIds = state.folders.map { it.id }.toSet()
        folderBounds.keys.filter { it !in visibleIds }.forEach { folderBounds.remove(it) }
    }

    com.example.webviewbrowser.core.ui.SubScreenScaffold(
        title = "ブックマーク",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "フォルダ作成")
            }
        },
    ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 4.dp)) {
        if (state.currentFolderId != null) {
            TextButton(onClick = { viewModel.openFolder(null) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text("ルートへ")
            }
        }
        if (state.folders.isEmpty() && state.bookmarks.isEmpty()) {
            com.example.webviewbrowser.core.ui.EmptyState("ブックマークはありません")
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.folders, key = { "f_${it.id}" }) { folder ->
                BookmarkFolderRow(
                    folder = folder,
                    isDropTarget = folder.id == hoveredFolderId,
                    onPositioned = { folderBounds[folder.id] = it },
                    onOpen = { viewModel.openFolder(folder.id) },
                    onDelete = { viewModel.deleteFolder(folder.id) },
                )
                HorizontalDivider()
            }
            items(state.bookmarks, key = { "b_${it.id}" }) { bookmark ->
                BookmarkRow(
                    bookmark = bookmark,
                    isDragging = bookmark.id == draggedBookmarkId,
                    onOpen = { onOpenUrl(bookmark.url) },
                    onDelete = { viewModel.deleteBookmark(bookmark.id) },
                    onDragStart = { draggedBookmarkId = bookmark.id },
                    onDragMove = { dragPosition = it },
                    onDragEnd = {
                        val targetFolderId = dragPosition?.let { position ->
                            folderBounds.entries.firstOrNull { (_, bounds) -> bounds.contains(position) }?.key
                        }
                        if (targetFolderId != null) {
                            viewModel.moveBookmarkToFolder(bookmark.id, targetFolderId)
                        }
                        draggedBookmarkId = null
                        dragPosition = null
                    },
                )
                HorizontalDivider()
            }
        }
    }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("フォルダ作成") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) viewModel.createFolder(name.trim())
                    showCreate = false
                }) { Text("作成") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("キャンセル") } },
        )
    }
}

@Composable
private fun BookmarkFolderRow(
    folder: BookmarkFolderEntity,
    isDropTarget: Boolean,
    onPositioned: (Rect) -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val backgroundColor = if (isDropTarget) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    ListItem(
        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
        headlineContent = { Text(folder.name) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除")
            }
        },
        colors = ListItemDefaults.colors(containerColor = backgroundColor),
        modifier = Modifier
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
            .clickable { onOpen() },
    )
}

@Composable
private fun BookmarkRow(
    bookmark: BookmarkEntity,
    isDragging: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    ListItem(
        headlineContent = { Text(bookmark.title.ifBlank { bookmark.url }, maxLines = 1) },
        supportingContent = { Text(bookmark.url, maxLines = 1) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除")
            }
        },
        modifier = Modifier
            .alpha(if (isDragging) 0.55f else 1f)
            .onGloballyPositioned { coordinates = it }
            .pointerInput(bookmark.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // 長押し開始位置を画面全体の座標へ変換して、フォルダ判定に使う。
                        coordinates?.localToRoot(offset)?.let(onDragMove)
                        onDragStart()
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { change, _ ->
                        change.consume()
                        val current = coordinates?.localToRoot(change.position)
                        if (current != null) {
                            onDragMove(current)
                        }
                    },
                )
            }
            .clickable { onOpen() },
    )
}
