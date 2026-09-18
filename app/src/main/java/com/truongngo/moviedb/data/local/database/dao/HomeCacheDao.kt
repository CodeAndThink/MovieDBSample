package com.truongngo.moviedb.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.truongngo.moviedb.data.local.model.CachedHomePage
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.local.model.HomeMovieEntity
import com.truongngo.moviedb.data.local.model.HomePageEntity
import com.truongngo.moviedb.data.mapper.toHomeMovieEntity
import com.truongngo.moviedb.data.mapper.toMovie
import com.truongngo.moviedb.data.network.model.MoviePage

@Dao
abstract class HomeCacheDao {
    @Query("SELECT * FROM home_pages WHERE feed = :feed AND language = :language AND region = :region")
    abstract suspend fun page(feed: String, language: String, region: String): HomePageEntity?

    @Query("SELECT * FROM home_movies WHERE feed = :feed AND language = :language AND region = :region ORDER BY position")
    abstract suspend fun movies(feed: String, language: String, region: String): List<HomeMovieEntity>

    @Upsert abstract suspend fun putPage(page: HomePageEntity)
    @Insert abstract suspend fun putMovies(movies: List<HomeMovieEntity>)

    @Query("DELETE FROM home_movies WHERE feed = :feed AND language = :language AND region = :region")
    abstract suspend fun deleteMovies(feed: String, language: String, region: String)

    @Transaction
    open suspend fun read(key: HomeCacheKey): CachedHomePage? {
        val page = page(key.feed, key.language, key.region) ?: return null
        return CachedHomePage(MoviePage(1, movies(key.feed, key.language, key.region).map { it.toMovie() },
            page.totalPages, page.totalResults), page.fetchedAt)
    }

    @Transaction
    open suspend fun replace(key: HomeCacheKey, response: MoviePage, fetchedAt: Long) {
        require(response.page == 1) { "Only Home page one is cached" }
        deleteMovies(key.feed, key.language, key.region)
        putPage(HomePageEntity(key, response.totalPages, response.totalResults, fetchedAt))
        putMovies(response.results.distinctBy { it.id }.mapIndexed { index, movie ->
            movie.toHomeMovieEntity(key, index)
        })
    }
}

