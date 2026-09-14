package com.truongngo.moviedb.presenter.detail

import androidx.lifecycle.SavedStateHandle
import com.truongngo.moviedb.domain.model.DownloadState
import com.truongngo.moviedb.domain.model.DownloadStatus
import com.truongngo.moviedb.domain.repository.DownloadRepository
import com.truongngo.moviedb.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun model() = DownloadViewModel(SavedStateHandle(mapOf("movieId" to 42)), ObserveDownload(repository), StartDownload(repository), CancelDownload(repository))

    @Test fun startsOnceAndRestoresProgressFromRepository() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        vm.onEvent(DownloadEvent.Start("Movie"))
        vm.onEvent(DownloadEvent.Start("Movie"))
        runCurrent()
        assertEquals(1, repository.starts)
        assertEquals(42, repository.lastId)
        repository.state.value = DownloadState(DownloadStatus.RUNNING, 65)
        runCurrent()
        assertEquals(65, vm.stateFlow.value.percent)
        val restored = model()
        runCurrent()
        assertEquals(65, restored.stateFlow.value.percent)
    }

    @Test fun cancellationAndCompletionComeFromRepository() = runTest(dispatcher) {
        val vm = model()
        runCurrent()
        vm.onEvent(DownloadEvent.Cancel)
        runCurrent()
        assertEquals(DownloadStatus.CANCELLED, vm.stateFlow.value.status)
        repository.state.value = DownloadState(DownloadStatus.COMPLETED, 100)
        runCurrent()
        assertFalse(vm.stateFlow.value.isActive)
        assertEquals(100, vm.stateFlow.value.percent)
    }

    private class FakeRepository : DownloadRepository {
        val state = MutableStateFlow(DownloadState())
        var starts = 0
        var lastId = 0
        override fun observe(movieId: Int) = state
        override suspend fun start(movieId: Int, title: String) { starts++; lastId = movieId; state.value = DownloadState(DownloadStatus.QUEUED) }
        override suspend fun cancel(movieId: Int) { state.value = DownloadState(DownloadStatus.CANCELLED) }
    }
}
