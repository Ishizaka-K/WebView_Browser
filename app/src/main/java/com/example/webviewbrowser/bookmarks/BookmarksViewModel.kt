package com.example.webviewbrowser.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webviewbrowser.bookmarks.data.BookmarkRepository
import com.example.webviewbrowser.bookmarks.domain.BookmarkUseCase
import com.example.webviewbrowser.data.db.entity.BookmarkEntity
import com.example.webviewbrowser.data.db.entity.BookmarkFolderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookmarksUiState(
    val currentFolderId: String? = null,
    val folders: List<BookmarkFolderEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: BookmarkRepository,
    private val useCase: BookmarkUseCase,
) : ViewModel() {

    private val currentFolderId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BookmarksUiState> = currentFolderId
        .flatMapLatest { folderId ->
            combine(
                repository.observeFolders(folderId),
                repository.observeBookmarks(folderId),
            ) { folders, bookmarks ->
                BookmarksUiState(folderId, folders, bookmarks)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksUiState())

    fun openFolder(id: String?) {
        currentFolderId.value = id
    }

    fun createFolder(name: String) {
        viewModelScope.launch { useCase.createFolder(name, currentFolderId.value) }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch { useCase.deleteFolder(id) }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch { useCase.removeBookmark(id) }
    }

    fun moveBookmarkToFolder(bookmarkId: String, folderId: String) {
        viewModelScope.launch {
            // ドロップ先フォルダへブックマークを移動する。
            useCase.moveBookmark(bookmarkId, folderId)
        }
    }
}
