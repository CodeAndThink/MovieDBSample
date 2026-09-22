package com.truongngo.moviedb.presenter.home

import com.truongngo.moviedb.domain.model.*
import com.truongngo.moviedb.domain.repository.MovieRepository
import com.truongngo.moviedb.domain.usecase.LoadHomeMoviesUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun viewModel() = HomeViewModel(LoadHomeMoviesUseCase(repository))

    @Test fun loadsSectionsIndependentlyAndLimitsBannerToTen() = runTest(dispatcher) {
        repository.topRated = { flowOf(MovieLoadResult.Error(MovieLoadError.CONNECTION)) }
        val vm = viewModel()
        runCurrent()
        assertEquals(10, vm.stateFlow.value.nowPlaying.size)
        assertEquals(2, vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.size)
        assertEquals(MovieLoadError.CONNECTION, vm.stateFlow.value.sections.getValue(MovieSection.TOP_RATED).error)
        assertFalse(vm.stateFlow.value.isRefreshing)
        assertEquals(4, repository.modes.size)
        assertTrue(repository.modes.all { it == HomeLoadMode.CACHE_FIRST })
    }

    @Test fun duplicateLoadMoreIsBlockedAndResultsAreDeduplicated() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        val pending = CompletableDeferred<HomeMoviePage>()
        repository.popular = { flow { emit(MovieLoadResult.Data(pending.await())) } }
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(1, 2), repository.popularCalls)
        pending.complete(page(2, listOf(2, 3), total = 2))
        runCurrent()
        val state = vm.stateFlow.value.sections.getValue(MovieSection.POPULAR)
        assertEquals(listOf(1, 2, 3), state.movies.map { it.id })
        assertFalse(state.canLoadMore)
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(1, 2), repository.popularCalls)
    }

    @Test fun failedPageKeepsContentAndRetriesTheSamePage() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        repository.popular = { flowOf(MovieLoadResult.Error(MovieLoadError.CONNECTION)) }
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(2, vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.size)
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(1, 2), repository.popularCalls)
        repository.popular = { flowOf(MovieLoadResult.Data(page(it, listOf(3, 4)))) }
        vm.onEvent(HomeEvent.RetrySection(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(1, 2, 2), repository.popularCalls)
        assertEquals(HomeLoadMode.FORCE_REFRESH, repository.modes.last())
        assertNull(vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).error)
    }

    @Test fun refreshDiscardsStaleLoadMoreEvenIfItIgnoresCancellation() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        val pending = CompletableDeferred<HomeMoviePage>()
        // Deliberately non-cooperative upstream proves the ViewModel generation guard independently.
        repository.popular = { object : Flow<MovieLoadResult> {
            override suspend fun collect(collector: FlowCollector<MovieLoadResult>) {
                withContext(NonCancellable) { collector.emit(MovieLoadResult.Data(pending.await())) }
            }
        } }
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        repository.popular = { flowOf(MovieLoadResult.Data(page(1, listOf(99)))) }
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        pending.complete(page(2, listOf(88)))
        runCurrent()
        val state = vm.stateFlow.value.sections.getValue(MovieSection.POPULAR)
        assertEquals(listOf(99), state.movies.map { it.id })
        assertEquals(1, state.page)
    }

    @Test fun refreshFailureRetriesFirstPageInsteadOfAppending() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        repository.popular = { flowOf(MovieLoadResult.Error(MovieLoadError.CONNECTION)) }
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        assertEquals(2, vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).page)
        assertTrue(repository.modes.takeLast(4).all { it == HomeLoadMode.FORCE_REFRESH })
        repository.popular = { flowOf(MovieLoadResult.Data(page(it, listOf(77)))) }
        vm.onEvent(HomeEvent.RetrySection(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(77), vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.map { it.id })
        assertEquals(listOf(1, 2, 1, 1), repository.popularCalls)
    }

    @Test fun emptyResponseStopsPaginationAndRefreshDoesNotDuplicateRequests() = runTest(dispatcher) {
        repository.popular = { flowOf(MovieLoadResult.Data(page(it, emptyList()))) }
        val vm = viewModel()
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        assertFalse(vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).canLoadMore)
        assertEquals(listOf(1), repository.popularCalls)
    }

    @Test fun bannerWrapsBothDirectionsIncludingSmallLists() {
        assertEquals(12, BannerPages.count(10))
        assertEquals(9, BannerPages.movieIndex(0, 10))
        assertEquals(0, BannerPages.movieIndex(11, 10))
        assertEquals(10, BannerPages.settledPosition(0, 10))
        assertEquals(1, BannerPages.settledPosition(11, 10))
        assertEquals(0, BannerPages.count(0))
        assertEquals(1, BannerPages.count(1))
        assertEquals(0, BannerPages.settledPosition(0, 1))
    }

    @Test fun serverErrorSurvivesIntoHomeStateWithoutDroppingMovies() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        val movies = vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies
        repository.popular = { flowOf(MovieLoadResult.Error(MovieLoadError.SERVER)) }
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        val section = vm.stateFlow.value.sections.getValue(MovieSection.POPULAR)
        assertEquals(MovieLoadError.SERVER, section.error)
        assertEquals(movies, section.movies)
        assertEquals(1, section.retryPage)
        assertFalse(section.isLoading)
    }

    private class FakeRepository : MovieRepository {
        val popularCalls = mutableListOf<Int>()
        val modes = mutableListOf<HomeLoadMode>()
        var popular: (Int) -> Flow<MovieLoadResult> = { flowOf(MovieLoadResult.Data(page(it, listOf(1, 2)))) }
        var topRated: (Int) -> Flow<MovieLoadResult> = { flowOf(MovieLoadResult.Data(page(it, listOf(3, 4)))) }
        override fun loadHomeMovies(feed: HomeFeed, page: Int, mode: HomeLoadMode): Flow<MovieLoadResult> {
            modes += mode
            return when (feed) {
                HomeFeed.POPULAR -> { popularCalls += page; popular(page) }
                HomeFeed.TOP_RATED -> topRated(page)
                HomeFeed.UPCOMING -> flowOf(MovieLoadResult.Data(page(page, listOf(5, 6))))
                HomeFeed.NOW_PLAYING -> flowOf(MovieLoadResult.Data(page(page, listOf(1, 1) + (2..20).toList())))
            }
        }
    }

    companion object {
        private fun page(page: Int, ids: List<Int>, total: Int = 3) = HomeMoviePage(page,
            ids.map { HomeMovie(it, "Movie $it", null, null, "2026-01-01", 8.0) }, total, 60)
    }
}
