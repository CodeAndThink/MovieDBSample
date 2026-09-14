package com.truongngo.moviedb.presenter.home

sealed interface HomeEvent {
    data object SearchClicked : HomeEvent
    data class MovieClicked(val movieId: Int) : HomeEvent
    data object Refresh : HomeEvent
    data object RetryNowPlaying : HomeEvent
    data class LoadMore(val section: MovieSection) : HomeEvent
    data class RetrySection(val section: MovieSection) : HomeEvent
}
