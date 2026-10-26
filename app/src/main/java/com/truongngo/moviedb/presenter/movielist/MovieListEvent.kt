package com.truongngo.moviedb.presenter.movielist

sealed interface MovieListEvent {
    data object BackClicked : MovieListEvent
    data class MovieClicked(val movieId: Int) : MovieListEvent
}
