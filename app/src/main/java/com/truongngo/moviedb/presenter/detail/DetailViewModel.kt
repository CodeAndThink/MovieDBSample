package com.truongngo.moviedb.presenter.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.NetworkException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

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
            DetailEvent.Retry -> loadMovie()
            DetailEvent.BackClicked -> effects.trySend(DetailEffect.NavigateBack)
        }
    }

    private fun loadMovie() {
        if (_stateFlow.value.isLoading) return
        if (movieId <= 0) {
            _stateFlow.value = DetailState(error = DetailError.INVALID_MOVIE)
            return
        }
        _stateFlow.value = DetailState(isLoading = true)
        viewModelScope.launch {
            try {
                _stateFlow.value = DetailState(movie = api.getMovieDetails(movieId))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val error = when (exception) {
                    is NetworkException if exception.httpCode in listOf(401, 403) -> DetailError.AUTHENTICATION
                    is NetworkException -> DetailError.GENERAL
                    is IOException -> DetailError.CONNECTION
                    else -> DetailError.GENERAL
                }
                _stateFlow.value = DetailState(error = error)
            }
        }
    }
}
