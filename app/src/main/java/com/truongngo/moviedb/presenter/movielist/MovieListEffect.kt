package com.truongngo.moviedb.presenter.movielist

sealed interface MovieListEffect {
    data object NavigateBack : MovieListEffect
    data class NavigateDetail(val movieId: Int) : MovieListEffect
}
