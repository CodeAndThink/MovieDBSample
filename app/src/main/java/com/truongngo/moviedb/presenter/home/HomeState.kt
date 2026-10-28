package com.truongngo.moviedb.presenter.home

import com.truongngo.moviedb.domain.model.HomeMovie
import com.truongngo.moviedb.domain.model.MovieLoadError

enum class MovieSection { POPULAR, TOP_RATED, UPCOMING }

data class HomeSectionState(
    val movies: List<HomeMovie> = emptyList(),
    val isLoading: Boolean = true,
    val error: MovieLoadError? = null,
)

data class HomeState(
    val sections: Map<MovieSection, HomeSectionState> = MovieSection.entries.associateWith { HomeSectionState() },
    val isRefreshing: Boolean = false,
    val nowPlaying: List<HomeMovie> = emptyList(),
    val isNowPlayingLoading: Boolean = false,
    val nowPlayingError: MovieLoadError? = null,
    val refreshVersion: Int = 0,
)
