package com.truongngo.moviedb.data.repository

import com.truongngo.moviedb.data.local.home.HomeCache
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.mapper.toHomeMoviePage
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.NetworkException
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
import java.io.IOException
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val api: ApiClients,
    private val cache: HomeCache,
) : MovieRepository {
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
        val error = when {
            exception is NetworkException && exception.httpCode in listOf(401, 403) -> MovieLoadError.AUTHENTICATION
            exception is NetworkException -> MovieLoadError.GENERAL
            exception is IOException -> MovieLoadError.CONNECTION
            else -> MovieLoadError.GENERAL
        }
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
