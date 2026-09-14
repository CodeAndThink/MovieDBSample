package com.truongngo.moviedb.presenter.detail

sealed interface DetailEffect {
    data object NavigateBack : DetailEffect
}
