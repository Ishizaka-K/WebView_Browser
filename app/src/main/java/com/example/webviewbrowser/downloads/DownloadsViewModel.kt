package com.example.webviewbrowser.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webviewbrowser.downloads.engine.DownloadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val useCase: DownloadUseCase,
) : ViewModel() {

    val downloads: StateFlow<List<DownloadProgress>> =
        useCase.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pause(id: String) = viewModelScope.launch { useCase.pause(id) }
    fun resume(id: String) = viewModelScope.launch { useCase.resume(id) }
    fun cancel(id: String) = viewModelScope.launch { useCase.cancel(id) }
    fun retry(id: String) = viewModelScope.launch { useCase.retry(id) }
}
