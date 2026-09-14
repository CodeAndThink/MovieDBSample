package com.truongngo.moviedb.presenter.search

sealed interface SearchEffect {
    data class NavigateDetail(val movieId: Int) : SearchEffect
    data object NavigateBack : SearchEffect
}
