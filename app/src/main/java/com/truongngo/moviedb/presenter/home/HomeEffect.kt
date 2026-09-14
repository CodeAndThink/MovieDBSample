package com.truongngo.moviedb.presenter.home

sealed interface HomeEffect {
    data object NavigateSearch : HomeEffect
    data class NavigateDetail(val movieId: Int) : HomeEffect
}
