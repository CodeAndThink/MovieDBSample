package com.truongngo.moviedb.presenter.detail

sealed interface DetailEffect {
    data class ShareMovie(val text: String) : DetailEffect
    data object NavigateBack : DetailEffect
}
