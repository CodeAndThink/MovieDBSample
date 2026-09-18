package com.truongngo.moviedb.presenter.home

import com.truongngo.moviedb.data.local.home.HomeCache
import com.truongngo.moviedb.data.local.model.CachedHomePage
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeApi()
    private val cache = FakeCache()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun loadsSectionsIndependentlyAndLimitsBannerToTen() = runTest(dispatcher) {
        api.topRated = { throw IOException("offline") }
        val vm = HomeViewModel(api, cache)
        runCurrent()
        val state = vm.stateFlow.value
        assertEquals(10, state.nowPlaying.size)
        assertEquals(2, state.sections.getValue(MovieSection.POPULAR).movies.size)
        assertEquals(HomeError.CONNECTION, state.sections.getValue(MovieSection.TOP_RATED).error)
        assertFalse(state.isRefreshing)
    }

    @Test fun duplicateLoadMoreIsBlockedAndResultsAreDeduplicated() = runTest(dispatcher) {
        val vm = HomeViewModel(api, cache)
        runCurrent()
        val pending = CompletableDeferred<MoviePage>()
        api.popular = { pending.await() }
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(1, 2), api.popularCalls)
        pending.complete(page(2, listOf(2, 3), total = 2))
        runCurrent()
        val state = vm.stateFlow.value.sections.getValue(MovieSection.POPULAR)
        assertEquals(listOf(1, 2, 3), state.movies.map { it.id })
        assertFalse(state.canLoadMore)
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(1, 2), api.popularCalls)
    }

    @Test fun failedPageKeepsContentAndRetriesTheSamePage() = runTest(dispatcher) {
        val vm = HomeViewModel(api, cache)
        runCurrent()
        api.popular = { throw IOException("offline") }
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(2, vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.size)
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(1, 2), api.popularCalls)
        api.popular = { page(it, listOf(3, 4)) }
        vm.onEvent(HomeEvent.RetrySection(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(1, 2, 2), api.popularCalls)
        assertNull(vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).error)
    }

    @Test fun refreshDiscardsStaleLoadMoreEvenIfItIgnoresCancellation() = runTest(dispatcher) {
        val vm = HomeViewModel(api, cache)
        runCurrent()
        val pending = CompletableDeferred<MoviePage>()
        api.popular = { withContext(NonCancellable) { pending.await() } }
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        api.popular = { page(1, listOf(99)) }
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        pending.complete(page(2, listOf(88)))
        runCurrent()
        val state = vm.stateFlow.value.sections.getValue(MovieSection.POPULAR)
        assertEquals(listOf(99), state.movies.map { it.id })
        assertEquals(1, state.page)
    }

    @Test fun refreshFailureRetriesFirstPageInsteadOfAppending() = runTest(dispatcher) {
        val vm = HomeViewModel(api, cache)
        runCurrent()
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        api.popular = { throw IOException("offline") }
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        assertEquals(2, vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).page)
        api.popular = { page(it, listOf(77)) }
        vm.onEvent(HomeEvent.RetrySection(MovieSection.POPULAR))
        runCurrent()
        val state = vm.stateFlow.value.sections.getValue(MovieSection.POPULAR)
        assertEquals(listOf(77), state.movies.map { it.id })
        assertEquals(listOf(1, 2, 1, 1), api.popularCalls)
    }

    @Test fun emptyResponseStopsPaginationAndRefreshDoesNotDuplicateRequests() = runTest(dispatcher) {
        api.popular = { page(it, emptyList()) }
        val vm = HomeViewModel(api, cache)
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        assertFalse(vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).canLoadMore)
        assertEquals(listOf(1), api.popularCalls)
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

    @Test fun freshCacheSkipsNetworkAndContinuesAtPageTwo() = runTest(dispatcher) {
        cache.pages[HomeCacheKey("POPULAR")] = CachedHomePage(page(1, listOf(42)), System.currentTimeMillis())
        val vm = HomeViewModel(api, cache)
        runCurrent()
        assertEquals(listOf(42), vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.map { it.id })
        assertTrue(api.popularCalls.isEmpty())
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertEquals(listOf(2), api.popularCalls)
        assertEquals(listOf(42), cache.pages.getValue(HomeCacheKey("POPULAR")).page.results.map { it.id })
    }

    @Test fun staleCacheIsVisibleWhileRefreshingAndSurvivesNetworkFailure() = runTest(dispatcher) {
        cache.pages[HomeCacheKey("POPULAR")] = CachedHomePage(page(1, listOf(42)), 0)
        val pending = CompletableDeferred<MoviePage>()
        api.popular = { pending.await() }
        val vm = HomeViewModel(api, cache)
        runCurrent()
        assertEquals(listOf(42), vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.map { it.id })
        assertTrue(vm.stateFlow.value.isRefreshing)
        pending.completeExceptionally(IOException("offline"))
        runCurrent()
        assertEquals(listOf(42), vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.map { it.id })
        assertEquals(HomeError.CONNECTION, vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).error)
    }

    @Test fun manualRefreshBypassesFreshCacheAndPersistsReplacement() = runTest(dispatcher) {
        cache.pages[HomeCacheKey("POPULAR")] = CachedHomePage(page(1, listOf(42)), System.currentTimeMillis())
        val vm = HomeViewModel(api, cache)
        runCurrent()
        vm.onEvent(HomeEvent.Refresh)
        runCurrent()
        assertEquals(listOf(1), api.popularCalls)
        assertEquals(listOf(1, 2), cache.pages.getValue(HomeCacheKey("POPULAR")).page.results.map { it.id })
    }

    @Test fun cacheFailureDoesNotHideNetworkResults() = runTest(dispatcher) {
        cache.fail = true
        val vm = HomeViewModel(api, cache)
        runCurrent()
        assertEquals(listOf(1, 2), vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).movies.map { it.id })
        assertNull(vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).error)
        assertFalse(vm.stateFlow.value.isRefreshing)
    }

    @Test fun reopeningHomeUsesAllFourCachedFeedsIncludingBanner() = runTest(dispatcher) {
        HomeViewModel(api, cache)
        runCurrent()
        assertEquals(setOf("POPULAR", "TOP_RATED", "UPCOMING", "NOW_PLAYING"), cache.pages.keys.map { it.feed }.toSet())
        val calls = api.calls.toList()
        val reopened = HomeViewModel(api, cache)
        runCurrent()
        assertEquals(calls, api.calls)
        assertEquals(10, reopened.stateFlow.value.nowPlaying.size)
        assertFalse(reopened.stateFlow.value.isNowPlayingLoading)
        assertFalse(reopened.stateFlow.value.isRefreshing)
    }

    @Test fun freshEmptyCacheDoesNotRefetchOrAllowPagination() = runTest(dispatcher) {
        cache.pages[HomeCacheKey("POPULAR")] = CachedHomePage(page(1, emptyList()), System.currentTimeMillis())
        val vm = HomeViewModel(api, cache)
        runCurrent()
        vm.onEvent(HomeEvent.LoadMore(MovieSection.POPULAR))
        runCurrent()
        assertTrue(api.popularCalls.isEmpty())
        assertFalse(vm.stateFlow.value.sections.getValue(MovieSection.POPULAR).canLoadMore)
    }

    @Test fun staleBannerSurvivesErrorAndRetryReplacesItsCache() = runTest(dispatcher) {
        cache.pages[HomeCacheKey("NOW_PLAYING")] = CachedHomePage(page(1, listOf(42, 42) + (1..20).toList()), 0)
        val pending = CompletableDeferred<MoviePage>()
        api.nowPlaying = { pending.await() }
        val vm = HomeViewModel(api, cache)
        runCurrent()
        assertTrue(vm.stateFlow.value.isNowPlayingLoading)
        assertEquals(listOf(42) + (1..9).toList(), vm.stateFlow.value.nowPlaying.map { it.id })
        pending.completeExceptionally(IOException("offline"))
        runCurrent()
        assertEquals(listOf(42) + (1..9).toList(), vm.stateFlow.value.nowPlaying.map { it.id })
        assertEquals(HomeError.CONNECTION, vm.stateFlow.value.nowPlayingError)
        api.nowPlaying = { page(1, listOf(77)) }
        vm.onEvent(HomeEvent.RetryNowPlaying)
        runCurrent()
        assertEquals(listOf(77), vm.stateFlow.value.nowPlaying.map { it.id })
        assertNull(vm.stateFlow.value.nowPlayingError)
        assertEquals(listOf(77), cache.pages.getValue(HomeCacheKey("NOW_PLAYING")).page.results.map { it.id })
    }

    @Test fun cacheExpiresAtThirtyMinutesAndRejectsFutureTimestamps() {
        val cached = CachedHomePage(page(1, emptyList()), 1000)
        assertTrue(cached.isFresh(1000))
        assertTrue(cached.isFresh(1000 + 30 * 60 * 1000L - 1))
        assertFalse(cached.isFresh(1000 + 30 * 60 * 1000L))
        assertFalse(cached.isFresh(999))
    }

    private class FakeCache : HomeCache {
        val pages = mutableMapOf<HomeCacheKey, CachedHomePage>()
        var fail = false
        override suspend fun read(key: HomeCacheKey): CachedHomePage? {
            if (fail) error("disk unavailable")
            return pages[key]
        }
        override suspend fun write(key: HomeCacheKey, page: MoviePage) {
            if (fail) error("disk unavailable")
            pages[key] = CachedHomePage(page, System.currentTimeMillis())
        }
    }

    private class FakeApi : ApiClients {
        val popularCalls = mutableListOf<Int>()
        val calls = mutableListOf<String>()
        var nowPlaying: suspend () -> MoviePage = { page(1, (1..20).toList()) }
        var popular: suspend (Int) -> MoviePage = { page(it, listOf(1, 2)) }
        var topRated: suspend (Int) -> MoviePage = { page(it, listOf(3, 4)) }
        override suspend fun getPopularMovies(page: Int, language: String, region: String?): MoviePage {
            calls += "POPULAR"
            popularCalls += page
            return popular(page)
        }
        override suspend fun getTopRatedMovies(page: Int, language: String, region: String?): MoviePage {
            calls += "TOP_RATED"
            return topRated(page)
        }
        override suspend fun getUpcomingMovies(page: Int, language: String, region: String?): MoviePage {
            calls += "UPCOMING"
            return page(page, listOf(5, 6))
        }
        override suspend fun getNowPlayingMovies(page: Int, language: String, region: String?): MoviePage {
            calls += "NOW_PLAYING"
            return nowPlaying()
        }
        override suspend fun searchMovies(query: String, page: Int, language: String, includeAdult: Boolean): MoviePage = error("Unused")
        override suspend fun getMovieDetails(movieId: Int, language: String): Movie = error("Unused")
        override suspend fun getMovieVideos(movieId: Int, language: String): VideoResponse = error("Unused")
        override suspend fun getMovieCredits(movieId: Int, language: String): CreditsResponse = error("Unused")
        override suspend fun getMovieGenres(language: String): GenreResponse = error("Unused")
    }

    companion object {
        private fun page(page: Int, ids: List<Int>, total: Int = 3) = MoviePage(page,
            ids.map { Movie(it, "Movie $it", null, null, null, null, "2026-01-01", 8.0, 10, null, null, null) }, total, 60)
    }
}
