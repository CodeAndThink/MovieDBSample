package com.truongngo.moviedb.domain.model

data class HomeMoviePage(
    val page: Int,
    val results: List<HomeMovie>,
    val totalPages: Int,
    val totalResults: Int,
)
