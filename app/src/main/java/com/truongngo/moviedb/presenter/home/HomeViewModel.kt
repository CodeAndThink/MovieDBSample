package com.truongngo.moviedb.presenter.home

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.model.HomeLoadMode
import com.truongngo.moviedb.domain.model.MovieLoadError
import com.truongngo.moviedb.domain.model.MovieLoadResult
import com.truongngo.moviedb.domain.usecase.LoadHomeMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
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
    private var bannerJob: Job? = null
    private val sectionJobs = mutableMapOf<MovieSection, Job>()
    private val observationJobs = mutableMapOf<MovieSection, Job>()
    private val observationErrors = mutableSetOf<MovieSection>()

    init {
        MovieSection.entries.forEach(::observeSection)
        refresh(useCache = true)
    }

    private fun observeSection(section: MovieSection) {
        observationJobs[section]?.cancel()
        observationErrors.remove(section)
        observationJobs[section] = viewModelScope.launch {
            loadHomeMovies.preview(HomeFeed.valueOf(section.name))
                .catch { error ->
                    if (error is CancellationException) throw error
                    observationErrors.add(section)
                    updateSection(section) { it.copy(error = MovieLoadError.GENERAL, isLoading = false) }
                }.collect { movies ->
                    updateSection(section) { it.copy(movies = movies.distinctBy { movie -> movie.id }.take(10)) }
                }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SeeMoreClicked -> effects.trySend(HomeEffect.NavigateMovieList(HomeFeed.valueOf(event.section.name)))
            is HomeEvent.RetrySection -> retrySection(event.section)
            HomeEvent.SearchClicked -> effects.trySend(HomeEffect.NavigateSearch)
            is HomeEvent.MovieClicked -> if (event.movieId > 0) { effects.trySend(HomeEffect.NavigateDetail(event.movieId)) }
            HomeEvent.Refresh -> refresh()
            HomeEvent.RetryNowPlaying -> retryNowPlaying()
        }
    }

    private fun refresh(useCache: Boolean = false) {
        if (_stateFlow.value.isRefreshing) return
        val currentGeneration = ++generation
        refreshJob?.cancel()
        bannerJob?.cancel()
        sectionJobs.values.forEach { it.cancel() }
        sectionJobs.clear()
        observationErrors.toList().forEach(::observeSection)
        MovieSection.entries.forEach { section -> updateSection(section) { it.copy(isLoading = true, error = null) } }
        _stateFlow.update { it.copy(isRefreshing = true, isNowPlayingLoading = true, nowPlayingError = null) }
        refreshJob = viewModelScope.launch {
            coroutineScope {
                launch { fetchNowPlaying(currentGeneration, useCache) }
                MovieSection.entries.forEach { section -> launch { fetchSection(section, currentGeneration, !useCache) } }
            }
            if (generation == currentGeneration) {
                _stateFlow.update { it.copy(isRefreshing = false, refreshVersion = it.refreshVersion + 1) }
            }
        }
    }

    private fun retrySection(section: MovieSection) {
        if (_stateFlow.value.isRefreshing || _stateFlow.value.sections.getValue(section).isLoading) return
        updateSection(section) { it.copy(isLoading = true, error = null) }
        if (section in observationErrors) observeSection(section)
        val currentGeneration = generation
        sectionJobs[section] = viewModelScope.launch { fetchSection(section, currentGeneration, true) }
    }

    private suspend fun fetchSection(section: MovieSection, currentGeneration: Int, force: Boolean) {
        val error = loadHomeMovies.refreshPreview(HomeFeed.valueOf(section.name), force)
        if (generation == currentGeneration) updateSection(section) { it.copy(isLoading = false, error = error ?: if (section in observationErrors) MovieLoadError.GENERAL else null) }
    }

    private fun updateSection(section: MovieSection, transform: (HomeSectionState) -> HomeSectionState) {
        _stateFlow.update { it.copy(sections = it.sections + (section to transform(it.sections.getValue(section)))) }
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
                    it.copy(isNowPlayingLoading = false, nowPlayingError = result.reason)
                }
            }
        }
    }

    private fun loadMode(useCache: Boolean) =
        if (useCache) HomeLoadMode.CACHE_FIRST else HomeLoadMode.FORCE_REFRESH

}
