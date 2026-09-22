package com.truongngo.moviedb.presenter.common

import androidx.annotation.StringRes
import com.truongngo.moviedb.R
import com.truongngo.moviedb.domain.model.MovieLoadError

@StringRes
fun MovieLoadError.messageResource(): Int = when (this) {
    MovieLoadError.AUTHENTICATION -> R.string.home_error_auth
    MovieLoadError.FORBIDDEN -> R.string.movie_error_forbidden
    MovieLoadError.NOT_FOUND -> R.string.movie_error_not_found
    MovieLoadError.RATE_LIMITED -> R.string.movie_error_rate_limited
    MovieLoadError.CONNECTION -> R.string.home_error_connection
    MovieLoadError.TIMEOUT -> R.string.movie_error_timeout
    MovieLoadError.SERVER -> R.string.movie_error_server
    MovieLoadError.GENERAL -> R.string.home_error_general
    MovieLoadError.INVALID_MOVIE -> R.string.detail_invalid_movie
}
