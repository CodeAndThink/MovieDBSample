package com.truongngo.moviedb.presenter.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.model.DownloadState
import com.truongngo.moviedb.domain.model.DownloadStatus
import com.truongngo.moviedb.domain.usecase.CancelDownload
import com.truongngo.moviedb.domain.usecase.ObserveDownload
import com.truongngo.moviedb.domain.usecase.StartDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DownloadEvent {
    data class Start(val title: String) : DownloadEvent
    data object Cancel : DownloadEvent
}

@HiltViewModel
class DownloadViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observe: ObserveDownload,
    private val start: StartDownload,
    private val cancel: CancelDownload,
) : ViewModel() {
    private val movieId = savedState.get<Int>("movieId") ?: 0
    private val state = MutableStateFlow(DownloadState())
    val stateFlow = state.asStateFlow()
    init {
        if (movieId > 0) viewModelScope.launch { observe(movieId).collect { state.value = it } }
    }
    fun onEvent(event: DownloadEvent) {
        if (movieId <= 0) return
        if (event is DownloadEvent.Start && state.value.isActive) return
        if (event is DownloadEvent.Start) state.value = DownloadState(DownloadStatus.QUEUED)
        viewModelScope.launch {
            try {
                when (event) {
                    is DownloadEvent.Start -> start(movieId, event.title)
                    DownloadEvent.Cancel -> cancel(movieId)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.value = DownloadState(DownloadStatus.FAILED) }
        }
    }
}
