package com.truongngo.moviedb.presenter.detail

import com.truongngo.moviedb.data.network.model.Movie
import com.truongngo.moviedb.domain.model.MovieLoadError

data class DetailState(
    val movie: Movie? = null,
    val isLoading: Boolean = false,
    val error: MovieLoadError? = null,
)
