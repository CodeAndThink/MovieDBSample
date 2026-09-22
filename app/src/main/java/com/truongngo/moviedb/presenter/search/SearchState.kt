package com.truongngo.moviedb.presenter.search

import com.truongngo.moviedb.data.network.model.Movie
import com.truongngo.moviedb.domain.model.MovieLoadError

data class SearchState(
    val query: String = "",
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val error: MovieLoadError? = null,
    val page: Int = 0,
    val canLoadMore: Boolean = false,
)
