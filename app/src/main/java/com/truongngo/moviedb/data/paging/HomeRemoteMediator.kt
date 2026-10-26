package com.truongngo.moviedb.data.paging

import androidx.paging.*
import com.truongngo.moviedb.data.local.database.dao.HomePagingDao
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.local.model.HomeFeedMovieEntity
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.NetworkException
import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.model.MovieLoadError
import com.truongngo.moviedb.domain.model.MoviePagingException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class HomeRemoteMediator(
    private val api: ApiClients,
    private val dao: HomePagingDao,
    private val key: HomeCacheKey,
    private val loadMutex: Mutex = Mutex(),
    private val clock: () -> Long = System::currentTimeMillis,
) : RemoteMediator<Int, HomeFeedMovieEntity>() {
    override suspend fun initialize(): InitializeAction = loadMutex.withLock {
        val cached = try {
            dao.remoteKey(key.feed, key.language, key.region)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) { null }
        val age = cached?.let { clock() - it.refreshedAt }
        if (age != null && age in 0 until 30 * 60 * 1000L) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, HomeFeedMovieEntity>): MediatorResult =
        loadMutex.withLock { loadPage(loadType) }

    private suspend fun loadPage(loadType: LoadType): MediatorResult {
        if (loadType == LoadType.PREPEND) return MediatorResult.Success(true)
        return try {
            val previous = dao.remoteKey(key.feed, key.language, key.region)
            val page = if (loadType == LoadType.REFRESH) 1 else previous?.nextPage
                ?: return MediatorResult.Success(true)
            val region = key.region.takeIf { it.isNotEmpty() }
            val response = when (HomeFeed.valueOf(key.feed)) {
                HomeFeed.POPULAR -> api.getPopularMovies(page, key.language, region)
                HomeFeed.TOP_RATED -> api.getTopRatedMovies(page, key.language, region)
                HomeFeed.UPCOMING -> api.getUpcomingMovies(page, key.language, region)
                HomeFeed.NOW_PLAYING -> api.getNowPlayingMovies(page, key.language, region)
            }
            currentCoroutineContext().ensureActive()
            check(response.page == page) { "Unexpected page in movie response" }
            if (!dao.savePage(key, response, previous, clock())) {
                return MediatorResult.Error(MoviePagingException(MovieLoadError.GENERAL))
            }
            MediatorResult.Success(response.results.isEmpty() || page >= minOf(response.totalPages, 500))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            val reason = when {
                exception is NetworkException && exception.httpCode in listOf(401, 403) -> MovieLoadError.AUTHENTICATION
                exception is NetworkException -> MovieLoadError.GENERAL
                exception is IOException -> MovieLoadError.CONNECTION
                else -> MovieLoadError.GENERAL
            }
            MediatorResult.Error(MoviePagingException(reason))
        }
    }
}
