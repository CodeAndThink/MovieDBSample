package com.truongngo.moviedb.data.repository

import com.truongngo.moviedb.data.local.home.HomeCache
import com.truongngo.moviedb.data.local.model.CachedHomePage
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.NetworkException
import com.truongngo.moviedb.data.network.model.*
import com.truongngo.moviedb.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MovieRepositoryImplTest {
    private val api = FakeApi()
    private val cache = FakeCache()
    private val repository = MovieRepositoryImpl(api, cache)

    @Test fun freshPagesSkipNetworkForEveryFeedIncludingEmptyPages() = runTest {
        for (feed in HomeFeed.entries) {
            cache.pages[HomeCacheKey(feed.name)] = CachedHomePage(page(1, emptyList()), System.currentTimeMillis())
            val values = repository.loadHomeMovies(feed, 1, HomeLoadMode.CACHE_FIRST).toList()
            assertEquals(1, values.size)
            val result = values.single() as MovieLoadResult.Data
            assertTrue(result.page.results.isEmpty())
            assertFalse(result.isRefreshing)
        }
        assertTrue(api.calls.isEmpty())
    }

    @Test fun stalePageEmitsBeforeNetworkAndSurvivesFailure() = runTest {
        val key = HomeCacheKey("POPULAR")
        cache.pages[key] = CachedHomePage(page(1, listOf(42)), 0)
        val pending = CompletableDeferred<MoviePage>()
        api.popular = { pending.await() }
        val results = mutableListOf<MovieLoadResult>()
        val job = launch { repository.loadHomeMovies(HomeFeed.POPULAR, 1, HomeLoadMode.CACHE_FIRST).toList(results) }
        runCurrent()
        assertEquals(listOf(42), (results.single() as MovieLoadResult.Data).page.results.map { it.id })
        assertTrue((results.single() as MovieLoadResult.Data).isRefreshing)
        pending.completeExceptionally(IOException("offline"))
        job.join()
        assertEquals(MovieLoadResult.Error(MovieLoadError.CONNECTION), results.last())
        assertEquals(listOf(42), cache.pages.getValue(key).page.results.map { it.id })
    }

    @Test fun forceRefreshReplacesFreshCacheAndWritesAllFeeds() = runTest {
        for (feed in HomeFeed.entries) {
            val key = HomeCacheKey(feed.name)
            cache.pages[key] = CachedHomePage(page(1, listOf(42)), System.currentTimeMillis())
            val result = repository.loadHomeMovies(feed, 1, HomeLoadMode.FORCE_REFRESH).single() as MovieLoadResult.Data
            assertFalse(result.isRefreshing)
            assertEquals(cache.pages.getValue(key).page.results.map { it.id }, result.page.results.map { it.id })
            assertFalse(result.page.results.any { it.id == 42 })
        }
        assertEquals(HomeFeed.entries.map { it.name }.toSet(), api.calls.toSet())
    }

    @Test fun pageTwoNeverReadsOrOverwritesPageOneCache() = runTest {
        val key = HomeCacheKey("POPULAR")
        cache.pages[key] = CachedHomePage(page(1, listOf(42)), System.currentTimeMillis())
        val result = repository.loadHomeMovies(HomeFeed.POPULAR, 2, HomeLoadMode.CACHE_FIRST).single() as MovieLoadResult.Data
        assertEquals(2, result.page.page)
        assertEquals(listOf(2), api.popularCalls)
        assertEquals(listOf(42), cache.pages.getValue(key).page.results.map { it.id })
    }

    @Test fun diskReadAndWriteFailuresStillReturnNetworkData() = runTest {
        cache.fail = true
        val result = repository.loadHomeMovies(HomeFeed.POPULAR, 1, HomeLoadMode.CACHE_FIRST).single() as MovieLoadResult.Data
        assertEquals(listOf(1, 2), result.page.results.map { it.id })
    }

    @Test fun transportAndHttpFailuresBecomeDomainErrors() = runTest {
        for ((exception, error) in listOf(
            IOException("offline") to MovieLoadError.CONNECTION,
            NetworkException(401, null, null) to MovieLoadError.AUTHENTICATION,
            NetworkException(403, null, null) to MovieLoadError.AUTHENTICATION,
            NetworkException(500, null, null) to MovieLoadError.GENERAL,
            IllegalStateException() to MovieLoadError.GENERAL,
        )) {
            api.popular = { throw exception }
            assertEquals(MovieLoadResult.Error(error), repository.loadHomeMovies(HomeFeed.POPULAR, 1, HomeLoadMode.FORCE_REFRESH).single())
        }
    }

    @Test fun cancelledRequestCannotPersistLateResponse() = runTest {
        val pending = CompletableDeferred<MoviePage>()
        api.popular = { withContext(NonCancellable) { pending.await() } }
        val results = mutableListOf<MovieLoadResult>()
        val job = launch { repository.loadHomeMovies(HomeFeed.POPULAR, 1, HomeLoadMode.FORCE_REFRESH).toList(results) }
        runCurrent()
        assertEquals(listOf(1), api.popularCalls)
        job.cancel()
        pending.complete(page(1, listOf(99)))
        job.join()
        assertTrue(results.isEmpty())
        assertTrue(cache.pages.isEmpty())
    }

    @Test fun collectorFailureIsNotTranslatedToNetworkError() = runTest {
        val failure = IllegalArgumentException("collector")
        try {
            repository.loadHomeMovies(HomeFeed.POPULAR, 1, HomeLoadMode.FORCE_REFRESH).collect { throw failure }
            fail("Expected collector exception")
        } catch (actual: IllegalArgumentException) {
            assertSame(failure, actual)
        }
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
