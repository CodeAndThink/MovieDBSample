package com.truongngo.moviedb.data.paging

import androidx.paging.*
import com.truongngo.moviedb.data.local.database.dao.HomePagingDao
import com.truongngo.moviedb.data.local.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
class HomeRemoteMediatorTest {
    private val dao = MemoryPagingDao()
    private val api = PagingTestApi()
    private val key = HomeCacheKey("POPULAR")
    private val state = PagingState<Int, HomeFeedMovieEntity>(emptyList(), null, PagingConfig(20), 0)
    private fun mediator(now: Long = 2_000_000) = HomeRemoteMediator(api, dao, key) { now }

    @Test fun refreshAppendAndReopenKeepOrderedDistinctMovies() = runTest {
        api.popular = { moviePage(it, if (it == 1) listOf(9, 2, 9) else listOf(2, 3)) }
        val mediator = mediator()
        mediator.load(LoadType.REFRESH, state)
        mediator.load(LoadType.APPEND, state)
        assertEquals(listOf(9, 2, 3), dao.rows.map { it.movie.movieId })
        assertEquals(3, dao.keys[key]?.nextPage)
        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, mediator().initialize())
        mediator().load(LoadType.APPEND, state)
        assertEquals(listOf(1, 2, 3), api.popularCalls)
    }

    @Test fun failedRefreshKeepsAllCachedPagesAndRetryStartsAtOne() = runTest {
        val mediator = mediator()
        mediator.load(LoadType.REFRESH, state)
        mediator.load(LoadType.APPEND, state)
        val before = dao.rows.toList()
        val previousKey = dao.keys[key]
        api.popular = { throw IOException("offline") }
        val error = mediator.load(LoadType.REFRESH, state)
        assertTrue(error is RemoteMediator.MediatorResult.Error)
        assertEquals(before, dao.rows)
        assertEquals(previousKey, dao.keys[key])
        api.popular = { moviePage(it, listOf(77)) }
        mediator.load(LoadType.REFRESH, state)
        assertEquals(listOf(77), dao.rows.map { it.movie.movieId })
        assertEquals(listOf(1, 2, 1, 1), api.popularCalls)
    }

    @Test fun failedAppendRetriesSamePageAndEmptyResponseEndsPagination() = runTest {
        val mediator = mediator()
        mediator.load(LoadType.REFRESH, state)
        api.popular = { throw IOException("offline") }
        mediator.load(LoadType.APPEND, state)
        api.popular = { moviePage(it, emptyList()) }
        assertTrue((mediator.load(LoadType.APPEND, state) as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        mediator.load(LoadType.APPEND, state)
        assertEquals(listOf(1, 2, 2), api.popularCalls)
        assertEquals(listOf(1, 2), dao.rows.map { it.movie.movieId })
    }

    @Test fun cancelledLateResponseCannotOverwriteCache() = runTest {
        val mediator = mediator()
        mediator.load(LoadType.REFRESH, state)
        val pending = CompletableDeferred<com.truongngo.moviedb.data.network.model.MoviePage>()
        api.popular = { withContext(NonCancellable) { pending.await() } }
        val job = launch { mediator.load(LoadType.APPEND, state) }
        runCurrent()
        job.cancel()
        pending.complete(moviePage(2, listOf(99)))
        job.join()
        assertEquals(listOf(1, 2), dao.rows.map { it.movie.movieId })
        assertEquals(2, dao.keys[key]?.nextPage)
    }

    @Test fun freshnessExpiresAtThirtyMinutesAndRejectsFutureTime() = runTest {
        mediator().load(LoadType.REFRESH, state)
        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, mediator(3_799_999).initialize())
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator(3_800_000).initialize())
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator(1_999_999).initialize())
    }

    @Test fun httpFailuresKeepTheirDomainClassification() = runTest {
        for ((code, expected) in listOf(401 to com.truongngo.moviedb.domain.model.MovieLoadError.AUTHENTICATION,
            403 to com.truongngo.moviedb.domain.model.MovieLoadError.FORBIDDEN,
            404 to com.truongngo.moviedb.domain.model.MovieLoadError.NOT_FOUND,
            429 to com.truongngo.moviedb.domain.model.MovieLoadError.RATE_LIMITED,
            500 to com.truongngo.moviedb.domain.model.MovieLoadError.SERVER)) {
            api.popular = { throw com.truongngo.moviedb.data.network.NetworkException(code, null, null) }
            val result = mediator().load(LoadType.REFRESH, state) as RemoteMediator.MediatorResult.Error
            assertEquals(expected, (result.throwable as com.truongngo.moviedb.domain.model.MoviePagingException).reason)
        }
    }

    @Test fun sharedFeedLockSerializesPreviewAndListRefreshes() = runTest {
        val mutex = kotlinx.coroutines.sync.Mutex()
        val preview = HomeRemoteMediator(api, dao, key, mutex)
        val list = HomeRemoteMediator(api, dao, key, mutex)
        val pending = CompletableDeferred<com.truongngo.moviedb.data.network.model.MoviePage>()
        api.popular = { if (api.popularCalls.size == 1) pending.await() else moviePage(1, listOf(77)) }
        val first = async { preview.load(LoadType.REFRESH, state) }
        runCurrent()
        val second = async { list.load(LoadType.REFRESH, state) }
        runCurrent()
        assertEquals(listOf(1), api.popularCalls)
        pending.complete(moviePage(1, listOf(42)))
        assertTrue(first.await() is RemoteMediator.MediatorResult.Success)
        assertTrue(second.await() is RemoteMediator.MediatorResult.Success)
        assertEquals(listOf(77), dao.rows.map { it.movie.movieId })
    }

    @Test fun rejectedWriteMustNotReportSuccess() = runTest {
        api.popular = {
            dao.keys[key] = HomeRemoteKey(key, 2, 1000, "newer")
            moviePage(1, listOf(99))
        }
        assertTrue(mediator().load(LoadType.REFRESH, state) is RemoteMediator.MediatorResult.Error)
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun prependNeverCallsApiAndPage500IsLast() = runTest {
        val mediator = mediator()
        assertTrue((mediator.load(LoadType.PREPEND, state) as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        dao.keys[key] = HomeRemoteKey(key, 500, 2_000_000, "seed")
        api.popular = { moviePage(it, listOf(99), total = 900) }
        assertTrue((mediator.load(LoadType.APPEND, state) as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        mediator.load(LoadType.APPEND, state)
        assertEquals(listOf(500), api.popularCalls)
    }
}

internal class MemoryPagingDao : HomePagingDao() {
    override fun observePreview(feed: String, language: String, region: String) = kotlinx.coroutines.flow.flow {
        emit(movies(feed, language, region).take(10))
    }
    val rows = mutableListOf<HomeFeedMovieEntity>()
    val keys = mutableMapOf<HomeCacheKey, HomeRemoteKey>()
    override fun pagingSource(feed: String, language: String, region: String): PagingSource<Int, HomeFeedMovieEntity> = error("Use Room integration test")
    override suspend fun remoteKey(feed: String, language: String, region: String) = keys[HomeCacheKey(feed, language, region)]
    override suspend fun movies(feed: String, language: String, region: String) = rows.filter { it.movie.key == HomeCacheKey(feed, language, region) }.sortedBy { it.movie.position }
    override suspend fun putKey(key: HomeRemoteKey) { keys[key.key] = key }
    override suspend fun insertMovies(movies: List<HomeFeedMovieEntity>) { rows += movies }
    override suspend fun clearMovies(feed: String, language: String, region: String) { rows.removeAll { it.movie.key == HomeCacheKey(feed, language, region) } }
}
