package com.truongngo.moviedb.data.repository

import androidx.paging.*
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap
import com.truongngo.moviedb.data.local.database.dao.HomePagingDao
import com.truongngo.moviedb.data.paging.HomeRemoteMediator
import com.truongngo.moviedb.data.mapper.toMovie
import com.truongngo.moviedb.data.mapper.toHomeMovie
import com.truongngo.moviedb.domain.model.HomeMovie
import kotlinx.coroutines.flow.map
import com.truongngo.moviedb.data.local.home.HomeCache
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.mapper.toHomeMoviePage
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.NetworkErrorMapper
import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.model.HomeLoadMode
import com.truongngo.moviedb.domain.model.MovieLoadError
import com.truongngo.moviedb.domain.model.MovieLoadResult
import com.truongngo.moviedb.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val api: ApiClients,
    private val cache: HomeCache,
    private val pagingDao: HomePagingDao,
) : MovieRepository {
    private val feedLocks = ConcurrentHashMap<HomeCacheKey, Mutex>()
    private fun mediator(key: HomeCacheKey) = HomeRemoteMediator(api, pagingDao, key, feedLocks.computeIfAbsent(key) { Mutex() })

    override fun observeHomePreview(feed: HomeFeed): Flow<List<HomeMovie>> {
        val key = HomeCacheKey(feed.name)
        return pagingDao.observePreview(key.feed, key.language, key.region)
            .map { rows -> rows.map { it.movie.toMovie().toHomeMovie() } }
    }

    @OptIn(ExperimentalPagingApi::class)
    override suspend fun refreshHomePreview(feed: HomeFeed, force: Boolean): MovieLoadError? {
        val mediator = mediator(HomeCacheKey(feed.name))
        if (!force && mediator.initialize() == RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH) return null
        val result = mediator.load(LoadType.REFRESH, PagingState(emptyList(), null, PagingConfig(20), 0))
        return (result as? RemoteMediator.MediatorResult.Error)?.let {
            (it.throwable as? com.truongngo.moviedb.domain.model.MoviePagingException)?.reason ?: MovieLoadError.GENERAL
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun pagedHomeMovies(feed: HomeFeed): Flow<PagingData<HomeMovie>> {
        val key = HomeCacheKey(feed.name)
        return Pager(
            config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 4, enablePlaceholders = false),
            remoteMediator = mediator(key),
            pagingSourceFactory = { pagingDao.pagingSource(key.feed, key.language, key.region) },
        ).flow.map { paging -> paging.map { it.movie.toMovie().toHomeMovie() } }
    }

    override fun loadHomeMovies(feed: HomeFeed, page: Int, mode: HomeLoadMode): Flow<MovieLoadResult> = flow<MovieLoadResult> {
        val key = HomeCacheKey(feed.name)
        if (page == 1 && mode == HomeLoadMode.CACHE_FIRST) {
            val cached = cacheOrNull { cache.read(key) }
            if (cached != null) {
                val fresh = cached.isFresh(System.currentTimeMillis())
                emit(MovieLoadResult.Data(cached.page.toHomeMoviePage(), isRefreshing = !fresh))
                if (fresh) return@flow
            }
        }
        val response = when (feed) {
            HomeFeed.NOW_PLAYING -> api.getNowPlayingMovies(page)
            HomeFeed.POPULAR -> api.getPopularMovies(page)
            HomeFeed.TOP_RATED -> api.getTopRatedMovies(page)
            HomeFeed.UPCOMING -> api.getUpcomingMovies(page)
        }
        // An API implementation may finish despite cancellation; it must not overwrite the cache.
        currentCoroutineContext().ensureActive()
        if (page == 1) cacheOrNull { cache.write(key, response) }
        emit(MovieLoadResult.Data(response.toHomeMoviePage()))
    }.catch { exception ->
        if (exception is CancellationException) throw exception
        currentCoroutineContext().ensureActive()
        val error = NetworkErrorMapper.toMovieLoadError(exception)
        emit(MovieLoadResult.Error(error))
    }

    private suspend fun <T> cacheOrNull(block: suspend () -> T): T? = try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        null
    }
}
