package com.truongngo.moviedb.domain.model

/** Movie loading failures; presentation owns localized messages and retry UI. */
enum class MovieLoadError {
    AUTHENTICATION, FORBIDDEN, NOT_FOUND, RATE_LIMITED,
    CONNECTION, TIMEOUT, SERVER, GENERAL, INVALID_MOVIE,
}
