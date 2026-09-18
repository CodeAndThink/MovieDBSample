package com.truongngo.moviedb.data.local.model

import androidx.room.Embedded
import androidx.room.Entity

/** Bounded page-one snapshots: each feed owns its rows, preserving order and avoiding orphan movies. */
@Entity(tableName = "home_movies", primaryKeys = ["feed", "language", "region", "position"])
data class HomeMovieEntity(
    @Embedded val key: HomeCacheKey,
    val position: Int,
    val movieId: Int,
    val title: String,
    val overview: String?,
    val originalTitle: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val genreIds: String?,
)
