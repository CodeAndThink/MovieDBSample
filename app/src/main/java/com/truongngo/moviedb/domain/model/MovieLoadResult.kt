package com.truongngo.moviedb.domain.model

sealed interface MovieLoadResult {
    /** isRefreshing means this cached page is followed by a network result or failure. */
    data class Data(val page: HomeMoviePage, val isRefreshing: Boolean = false) : MovieLoadResult
    data class Error(val reason: MovieLoadError) : MovieLoadResult
}
