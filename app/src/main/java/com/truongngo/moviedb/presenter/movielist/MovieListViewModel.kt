package com.truongngo.moviedb.presenter.movielist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.usecase.LoadHomeMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(savedState: SavedStateHandle, loadMovies: LoadHomeMoviesUseCase) : ViewModel() {
    val feed = HomeFeed.valueOf(requireNotNull(savedState.get<String>("feed"))).also { require(it != HomeFeed.NOW_PLAYING) }
    val movies = loadMovies.paged(feed).cachedIn(viewModelScope)
    private val effects = Channel<MovieListEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    fun onEvent(event: MovieListEvent) {
        when (event) {
            MovieListEvent.BackClicked -> effects.trySend(MovieListEffect.NavigateBack)
            is MovieListEvent.MovieClicked -> if (event.movieId > 0) effects.trySend(MovieListEffect.NavigateDetail(event.movieId))
        }
    }
}
