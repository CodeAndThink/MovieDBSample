package com.truongngo.moviedb.presenter.home

import com.truongngo.moviedb.domain.model.MovieLoadError
import com.truongngo.moviedb.domain.usecase.LoadHomeMoviesUseCase
import com.truongngo.moviedb.data.repository.MovieRepositoryImpl
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
class HomeCacheIntegrationTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeApi()
    private val cache = FakeCache()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun staleBannerSurvivesErrorAndRetryReplacesItsCache() = runTest(dispatcher) {
        cache.pages[HomeCacheKey("NOW_PLAYING")] = CachedHomePage(page(1, listOf(42, 42) + (1..20).toList()), 0)
        val pending = CompletableDeferred<MoviePage>()
        api.nowPlaying = { pending.await() }
        val vm = HomeViewModel(LoadHomeMoviesUseCase(MovieRepositoryImpl(api, cache, com.truongngo.moviedb.data.paging.MemoryPagingDao())))
        runCurrent()
        assertTrue(vm.stateFlow.value.isNowPlayingLoading)
        assertEquals(listOf(42) + (1..9).toList(), vm.stateFlow.value.nowPlaying.map { it.id })
        pending.completeExceptionally(IOException("offline"))
        runCurrent()
        assertEquals(listOf(42) + (1..9).toList(), vm.stateFlow.value.nowPlaying.map { it.id })
        assertEquals(MovieLoadError.CONNECTION, vm.stateFlow.value.nowPlayingError)
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
