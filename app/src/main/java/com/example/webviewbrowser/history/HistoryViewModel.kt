package com.example.webviewbrowser.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webviewbrowser.data.db.entity.HistoryEntity
import com.example.webviewbrowser.history.domain.HistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val query: String = "",
    val entries: List<HistoryEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val useCase: HistoryUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val entries: StateFlow<List<HistoryEntity>> = query
        .flatMapLatest { q ->
            if (q.isBlank()) useCase.observeAll() else useCase.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentQuery: StateFlow<String> = query

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun delete(id: String) {
        viewModelScope.launch { useCase.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { useCase.clearAll() }
    }
}
