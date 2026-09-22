package com.truongngo.moviedb.presenter.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.NetworkErrorMapper
import com.truongngo.moviedb.domain.model.MovieLoadError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val api: ApiClients,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val movieId = savedStateHandle.get<Int>("movieId") ?: 0
    private val _stateFlow = MutableStateFlow(DetailState())
    val stateFlow = _stateFlow.asStateFlow()
    private val effects = Channel<DetailEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    init { loadMovie() }

    fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.ShareClicked -> {
                val movie = _stateFlow.value.movie ?: return
                val text = if (movie.id > 0) "https://www.themoviedb.org/movie/${movie.id}" else movie.title
                if (text.isNotBlank()) effects.trySend(DetailEffect.ShareMovie(text))
            }
            DetailEvent.Retry -> loadMovie()
            DetailEvent.BackClicked -> effects.trySend(DetailEffect.NavigateBack)
        }
    }

    private fun loadMovie() {
        if (_stateFlow.value.isLoading) return
        if (movieId <= 0) {
            _stateFlow.value = DetailState(error = MovieLoadError.INVALID_MOVIE)
            return
        }
        _stateFlow.value = DetailState(isLoading = true)
        viewModelScope.launch {
            try {
                _stateFlow.value = DetailState(movie = api.getMovieDetails(movieId))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val error = NetworkErrorMapper.toMovieLoadError(exception)
                _stateFlow.value = DetailState(error = error)
            }
        }
    }
}
