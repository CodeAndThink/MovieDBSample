package com.truongngo.moviedb.presenter.detail

sealed interface DetailEvent {
    data object ShareClicked : DetailEvent
    data object Retry : DetailEvent
    data object BackClicked : DetailEvent
}
