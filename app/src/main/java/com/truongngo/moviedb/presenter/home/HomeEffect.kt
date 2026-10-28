package com.truongngo.moviedb.presenter.home

sealed interface HomeEffect {
    data class NavigateMovieList(val feed: com.truongngo.moviedb.domain.model.HomeFeed) : HomeEffect
    data object NavigateSearch : HomeEffect
    data class NavigateDetail(val movieId: Int) : HomeEffect
}
