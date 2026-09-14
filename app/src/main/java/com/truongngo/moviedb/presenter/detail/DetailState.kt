package com.truongngo.moviedb.presenter.detail

import com.truongngo.moviedb.data.network.model.Movie

enum class DetailError { CONNECTION, AUTHENTICATION, GENERAL, INVALID_MOVIE }

data class DetailState(
    val movie: Movie? = null,
    val isLoading: Boolean = false,
    val error: DetailError? = null,
)
