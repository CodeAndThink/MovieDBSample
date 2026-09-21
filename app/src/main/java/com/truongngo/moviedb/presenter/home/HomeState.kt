package com.truongngo.moviedb.presenter.home

import com.truongngo.moviedb.domain.model.HomeMovie

enum class MovieSection { POPULAR, TOP_RATED, UPCOMING }
enum class HomeError { CONNECTION, AUTHENTICATION, GENERAL }

data class MovieSectionState(
    val movies: List<HomeMovie> = emptyList(),
    val page: Int = 0,
    val canLoadMore: Boolean = true,
    val isLoading: Boolean = false,
    val error: HomeError? = null,
    val retryPage: Int? = null,
)

data class HomeState(
    val isRefreshing: Boolean = false,
    val nowPlaying: List<HomeMovie> = emptyList(),
    val isNowPlayingLoading: Boolean = false,
    val nowPlayingError: HomeError? = null,
    val sections: Map<MovieSection, MovieSectionState> = MovieSection.entries.associateWith { MovieSectionState() },
    val refreshVersion: Int = 0,
)
