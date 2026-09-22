package com.truongngo.moviedb.presenter.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.NetworkErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val api: ApiClients,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow(SearchState())
    val stateFlow = state.asStateFlow()
    private val effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()
    private var job: Job? = null
    private var generation = 0

    init { changeQuery(savedState["query"] ?: "") }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> changeQuery(event.query)
            is SearchEvent.MovieClicked -> if (event.movieId > 0) effects.trySend(SearchEffect.NavigateDetail(event.movieId))
            SearchEvent.BackClicked -> effects.trySend(SearchEffect.NavigateBack)
            SearchEvent.Submit -> if (state.value.query.isNotBlank() && state.value.page == 0) request(1, 0)
            SearchEvent.Retry -> if (!state.value.isLoading && state.value.error != null) request(state.value.page + 1, 0)
            SearchEvent.LoadMore -> if (!state.value.isLoading && state.value.error == null && state.value.canLoadMore) request(state.value.page + 1, 0)
        }
    }

    private fun changeQuery(query: String) {
        savedState["query"] = query
        if (query.trim() == state.value.query.trim()) {
            state.value = state.value.copy(query = query)
            return
        }
        ++generation
        job?.cancel()
        state.value = SearchState(query = query)
        if (query.isNotBlank()) request(1, 500)
    }

    private fun request(page: Int, waitMs: Long) {
        val query = state.value.query.trim()
        if (query.isEmpty()) return
        val version = ++generation
        job?.cancel()
        state.value = state.value.copy(isLoading = true, error = null)
        job = viewModelScope.launch {
            try {
                delay(waitMs)
                val response = api.searchMovies(query, page)
                if (version == generation) state.value = state.value.copy(
                    movies = ((if (page == 1) emptyList() else state.value.movies) + response.results).distinctBy { it.id },
                    isLoading = false,
                    page = page,
                    canLoadMore = response.results.isNotEmpty() && page < minOf(response.totalPages, 500),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (version == generation) state.value = state.value.copy(isLoading = false, error = NetworkErrorMapper.toMovieLoadError(exception))
            }
        }
    }
}
