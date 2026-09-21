package com.truongngo.moviedb.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.local.model.HomeMovieEntity
import com.truongngo.moviedb.data.network.model.Movie

fun HomeMovieEntity.toMovie(): Movie = Movie(
    movieId, title, overview, originalTitle, posterPath, backdropPath,
    releaseDate, voteAverage, voteCount,
    genreIds?.let { Gson().fromJson(it, object : TypeToken<List<Int>>() {}.type) }, null, null,
)

fun Movie.toHomeMovieEntity(key: HomeCacheKey, position: Int): HomeMovieEntity = HomeMovieEntity(
    key, position, id, title, overview, originalTitle, posterPath,
    backdropPath, releaseDate, voteAverage, voteCount,
    genreIds?.let { Gson().toJson(it) },
)
