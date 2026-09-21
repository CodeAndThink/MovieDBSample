package com.truongngo.moviedb.domain.model

data class HomeMovie(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
)
