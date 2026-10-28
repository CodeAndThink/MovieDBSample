package com.truongngo.moviedb.data.local.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.truongngo.moviedb.data.local.model.*
import com.truongngo.moviedb.data.mapper.toHomeMovieEntity
import com.truongngo.moviedb.data.network.model.MoviePage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.UUID

@Dao
abstract class HomePagingDao {
    @Query("SELECT * FROM home_feed_movies WHERE feed = :feed AND language = :language AND region = :region ORDER BY position LIMIT 10")
    abstract fun observePreview(feed: String, language: String, region: String): kotlinx.coroutines.flow.Flow<List<HomeFeedMovieEntity>>

    @Query("SELECT * FROM home_feed_movies WHERE feed = :feed AND language = :language AND region = :region ORDER BY position")
    abstract fun pagingSource(feed: String, language: String, region: String): PagingSource<Int, HomeFeedMovieEntity>

    @Query("SELECT * FROM home_remote_keys WHERE feed = :feed AND language = :language AND region = :region")
    abstract suspend fun remoteKey(feed: String, language: String, region: String): HomeRemoteKey?

    @Query("SELECT * FROM home_feed_movies WHERE feed = :feed AND language = :language AND region = :region ORDER BY position")
    abstract suspend fun movies(feed: String, language: String, region: String): List<HomeFeedMovieEntity>

    @Upsert abstract suspend fun putKey(key: HomeRemoteKey)
    @Insert abstract suspend fun insertMovies(movies: List<HomeFeedMovieEntity>)

    @Query("DELETE FROM home_feed_movies WHERE feed = :feed AND language = :language AND region = :region")
    abstract suspend fun clearMovies(feed: String, language: String, region: String)

    /** A late response from another generation must not replace or append to newer data. */
    @Transaction
    open suspend fun savePage(key: HomeCacheKey, response: MoviePage, expected: HomeRemoteKey?, now: Long): Boolean {
        currentCoroutineContext().ensureActive()
        if (remoteKey(key.feed, key.language, key.region) != expected) return false
        if (response.page == 1) clearMovies(key.feed, key.language, key.region)
        val existing = movies(key.feed, key.language, key.region)
        val ids = existing.mapTo(mutableSetOf()) { it.movie.movieId }
        val start = (existing.lastOrNull()?.movie?.position ?: -1) + 1
        insertMovies(response.results.filter { ids.add(it.id) }.mapIndexed { index, movie ->
            HomeFeedMovieEntity(movie.toHomeMovieEntity(key, start + index))
        })
        val end = response.results.isEmpty() || response.page >= minOf(response.totalPages, 500)
        putKey(HomeRemoteKey(key, if (end) null else response.page + 1,
            if (response.page == 1) now else expected?.refreshedAt ?: now,
            if (response.page == 1) UUID.randomUUID().toString() else requireNotNull(expected).generation))
        return true
    }
}
