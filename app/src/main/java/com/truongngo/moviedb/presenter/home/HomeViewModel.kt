package com.truongngo.moviedb.presenter.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.model.HomeLoadMode
import com.truongngo.moviedb.domain.model.HomeMoviePage
import com.truongngo.moviedb.domain.model.MovieLoadError
import com.truongngo.moviedb.domain.model.MovieLoadResult
import com.truongngo.moviedb.domain.usecase.LoadHomeMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val loadHomeMovies: LoadHomeMoviesUseCase) : ViewModel() {
    private val _stateFlow = MutableStateFlow(HomeState())
    val stateFlow = _stateFlow.asStateFlow()
    private val effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()
    private var generation = 0
    private var refreshJob: Job? = null
    private val pageJobs = mutableMapOf<MovieSection, Job>()
    private var bannerJob: Job? = null

    init {
        refresh(useCache = true)
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

    private fun refresh(useCache: Boolean = false) {
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
                launch { fetchNowPlaying(currentGeneration, useCache) }
                MovieSection.entries.forEach { section ->
                    launch { fetchSection(section, page = 1, currentGeneration, useCache) }
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

    private suspend fun fetchNowPlaying(currentGeneration: Int, useCache: Boolean = false) {
        loadHomeMovies(HomeFeed.NOW_PLAYING, mode = loadMode(useCache)).collect { result ->
            if (generation != currentGeneration) return@collect
            when (result) {
                is MovieLoadResult.Data -> _stateFlow.update {
                    it.copy(nowPlaying = result.page.results.distinctBy { movie -> movie.id }.take(10),
                        isNowPlayingLoading = result.isRefreshing, nowPlayingError = null)
                }
                is MovieLoadResult.Error -> _stateFlow.update {
                    it.copy(isNowPlayingLoading = false, nowPlayingError = result.reason.toHomeError())
                }
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

    private suspend fun fetchSection(section: MovieSection, page: Int, currentGeneration: Int, useCache: Boolean = false) {
        val feed = when (section) {
            MovieSection.POPULAR -> HomeFeed.POPULAR
            MovieSection.TOP_RATED -> HomeFeed.TOP_RATED
            MovieSection.UPCOMING -> HomeFeed.UPCOMING
        }
        loadHomeMovies(feed, page, loadMode(useCache)).collect { result ->
            if (generation != currentGeneration) return@collect
            when (result) {
                is MovieLoadResult.Data -> applyPage(section, result.page, loading = result.isRefreshing)
                is MovieLoadResult.Error -> updateSection(section) {
                    // Keep content and retry the requested page, including failed page-one refreshes.
                    it.copy(isLoading = false, error = result.reason.toHomeError(), retryPage = page)
                }
            }
        }
    }

    private fun loadMode(useCache: Boolean) =
        if (useCache) HomeLoadMode.CACHE_FIRST else HomeLoadMode.FORCE_REFRESH

    private fun applyPage(section: MovieSection, response: HomeMoviePage, loading: Boolean = false) {
        updateSection(section) { previous ->
            previous.copy(
                movies = ((if (response.page == 1) emptyList() else previous.movies) + response.results).distinctBy { it.id },
                page = response.page,
                canLoadMore = response.results.isNotEmpty() && response.page < minOf(response.totalPages, 500),
                isLoading = loading,
                error = null,
                retryPage = null,
            )
        }
    }

    private fun updateSection(section: MovieSection, transform: (MovieSectionState) -> MovieSectionState) {
        _stateFlow.update { it.copy(sections = it.sections + (section to transform(it.sections.getValue(section)))) }
    }

    private fun MovieLoadError.toHomeError(): HomeError = when (this) {
        MovieLoadError.AUTHENTICATION -> HomeError.AUTHENTICATION
        MovieLoadError.CONNECTION -> HomeError.CONNECTION
        MovieLoadError.GENERAL -> HomeError.GENERAL
    }
}
