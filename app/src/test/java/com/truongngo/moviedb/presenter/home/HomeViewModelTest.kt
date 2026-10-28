package com.truongngo.moviedb.presenter.home

import androidx.paging.PagingData
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

    @Test fun initializesOnlyBannerOutsidePaging() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        assertEquals(10, vm.stateFlow.value.nowPlaying.size)
        assertTrue(repository.popularCalls.isEmpty())
        assertEquals(listOf(HomeLoadMode.CACHE_FIRST), repository.modes)
    }

    @Test fun homeLimitsEverySectionToTenAndNeverCreatesPager() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        assertEquals(0, repository.pagerCalls)
        MovieSection.entries.forEach { assertEquals(10, vm.stateFlow.value.sections.getValue(it).movies.size) }
        assertEquals(3, repository.refreshes.size)
        assertTrue(repository.refreshes.none { it.second })
    }

    @Test fun retryRestartsPreviewObservationAfterDatabaseReadFailure() = runTest(dispatcher) {
        repository.failPreview = true
        val vm = viewModel()
        runCurrent()
        assertEquals(HomeError.GENERAL, vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).error)
        repository.failPreview = false
        vm.onEvent(HomeEvent.RetrySection(MovieSection.POPULAR))
        runCurrent()
        assertEquals(10, vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.size)
        assertNull(vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).error)
    }

    @Test fun seeMoreCarriesTheSelectedFeed() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEvent(HomeEvent.SeeMoreClicked(MovieSection.TOP_RATED))
        assertEquals(HomeEffect.NavigateMovieList(HomeFeed.TOP_RATED), vm.effectFlow.first())
    }

    @Test fun refreshBypassesCacheAndBlocksDuplicateBannerRequests() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onEvent(HomeEvent.Refresh)
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        assertEquals(listOf(HomeLoadMode.CACHE_FIRST, HomeLoadMode.FORCE_REFRESH), repository.modes)
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
        var pagerCalls = 0
        val refreshes = mutableListOf<Pair<HomeFeed, Boolean>>()
        override fun pagedHomeMovies(feed: HomeFeed): Flow<PagingData<HomeMovie>> {
            pagerCalls++
            return flowOf(PagingData.empty())
        }
        var failPreview = false
        override fun observeHomePreview(feed: HomeFeed) = flow {
            if (failPreview) error("database unavailable")
            emit((1..20).map { HomeMovie(it, "Movie $it", null, null, null, 8.0) })
        }
        override suspend fun refreshHomePreview(feed: HomeFeed, force: Boolean): MovieLoadError? {
            refreshes += feed to force
            return null
        }
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
