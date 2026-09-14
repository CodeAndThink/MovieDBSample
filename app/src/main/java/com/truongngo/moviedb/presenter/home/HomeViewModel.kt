package com.truongngo.moviedb.presenter.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.NetworkException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val api: ApiClients) : ViewModel() {
    private val _stateFlow = MutableStateFlow(HomeState())
    val stateFlow = _stateFlow.asStateFlow()
    private val effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()
    private var generation = 0
    private var refreshJob: Job? = null
    private val pageJobs = mutableMapOf<MovieSection, Job>()
    private var bannerJob: Job? = null

    init {
        refresh()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.SearchClicked -> effects.trySend(HomeEffect.NavigateSearch)
            is HomeEvent.MovieClicked -> if (event.movieId > 0) { effects.trySend(HomeEffect.NavigateDetail(event.movieId)) }
            HomeEvent.Refresh -> refresh()
            HomeEvent.RetryNowPlaying -> retryNowPlaying()
            is HomeEvent.LoadMore -> loadMore(event.section, retry = false)
            is HomeEvent.RetrySection -> loadMore(event.section, retry = true)
        }
    }

    private fun refresh() {
        if (_stateFlow.value.isRefreshing) return
        val currentGeneration = ++generation
        refreshJob?.cancel()
        bannerJob?.cancel()
        pageJobs.values.forEach { it.cancel() }
        pageJobs.clear()
        _stateFlow.update { state ->
            state.copy(isRefreshing = true, isNowPlayingLoading = true, nowPlayingError = null,
                sections = state.sections.mapValues { (_, section) -> section.copy(isLoading = true, error = null) })
        }
        refreshJob = viewModelScope.launch {
            coroutineScope {
                launch { fetchNowPlaying(currentGeneration) }
                MovieSection.entries.forEach { section ->
                    launch { fetchSection(section, page = 1, currentGeneration) }
                }
            }
            if (generation == currentGeneration) {
                _stateFlow.update { it.copy(isRefreshing = false, refreshVersion = it.refreshVersion + 1) }
            }
        }
    }

    private fun retryNowPlaying() {
        if (_stateFlow.value.isRefreshing || _stateFlow.value.isNowPlayingLoading) return
        val currentGeneration = generation
        _stateFlow.update { it.copy(isNowPlayingLoading = true, nowPlayingError = null) }
        bannerJob = viewModelScope.launch { fetchNowPlaying(currentGeneration) }
    }

    private suspend fun fetchNowPlaying(currentGeneration: Int) {
        try {
            val movies = api.getNowPlayingMovies().results.distinctBy { it.id }.take(10)
            if (generation == currentGeneration) _stateFlow.update {
                it.copy(nowPlaying = movies, isNowPlayingLoading = false, nowPlayingError = null)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (generation == currentGeneration) _stateFlow.update {
                it.copy(isNowPlayingLoading = false, nowPlayingError = exception.toHomeError())
            }
        }
    }

    private fun loadMore(section: MovieSection, retry: Boolean) {
        val state = _stateFlow.value
        val list = state.sections.getValue(section)
        if (state.isRefreshing || list.isLoading || (!list.canLoadMore && list.error == null)) return
        if (list.error != null && !retry) return
        val currentGeneration = generation
        updateSection(section) { it.copy(isLoading = true, error = null) }
        pageJobs[section] = viewModelScope.launch {
            fetchSection(section, list.retryPage ?: (list.page + 1), currentGeneration)
        }
    }

    private suspend fun fetchSection(section: MovieSection, page: Int, currentGeneration: Int) {
        try {
            val response = when (section) {
                MovieSection.POPULAR -> api.getPopularMovies(page)
                MovieSection.TOP_RATED -> api.getTopRatedMovies(page)
                MovieSection.UPCOMING -> api.getUpcomingMovies(page)
            }
            if (generation == currentGeneration) updateSection(section) { previous ->
                previous.copy(
                    movies = ((if (page == 1) emptyList() else previous.movies) + response.results).distinctBy { it.id },
                    page = page,
                    canLoadMore = response.results.isNotEmpty() && page < minOf(response.totalPages, 500),
                    isLoading = false,
                    error = null,
                    retryPage = null,
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (generation == currentGeneration) updateSection(section) {
                // Keep the last successful page and its films so a retry requests the same next page.
                it.copy(isLoading = false, error = exception.toHomeError(), retryPage = page)
            }
        }
    }

    private fun updateSection(section: MovieSection, transform: (MovieSectionState) -> MovieSectionState) {
        _stateFlow.update { it.copy(sections = it.sections + (section to transform(it.sections.getValue(section)))) }
    }

    private fun Exception.toHomeError(): HomeError = when {
        this is NetworkException && httpCode in listOf(401, 403) -> HomeError.AUTHENTICATION
        this is NetworkException -> HomeError.GENERAL
        this is IOException -> HomeError.CONNECTION
        else -> HomeError.GENERAL
    }
}
