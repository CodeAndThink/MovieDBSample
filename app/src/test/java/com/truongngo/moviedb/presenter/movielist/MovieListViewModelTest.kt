package com.truongngo.moviedb.presenter.movielist

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.truongngo.moviedb.domain.model.*
import com.truongngo.moviedb.domain.repository.MovieRepository
import com.truongngo.moviedb.domain.usecase.LoadHomeMoviesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private val repository = object : MovieRepository {
        override fun observeHomePreview(feed: HomeFeed) = flowOf(emptyList<HomeMovie>())
        override suspend fun refreshHomePreview(feed: HomeFeed, force: Boolean): MovieLoadError? = null
        override fun loadHomeMovies(feed: HomeFeed, page: Int, mode: HomeLoadMode): Flow<MovieLoadResult> = error("Unexpected banner request")
        override fun pagedHomeMovies(feed: HomeFeed) = flowOf(PagingData.from((1..30).map {
            HomeMovie(it, feed.name, null, null, null, 8.0)
        }))
    }

    @Test fun selectedFeedLoadsFullListAndEmitsNavigation() = runTest(dispatcher) {
        for (feed in listOf(HomeFeed.POPULAR, HomeFeed.TOP_RATED, HomeFeed.UPCOMING)) {
            val model = MovieListViewModel(SavedStateHandle(mapOf("feed" to feed.name)), LoadHomeMoviesUseCase(repository))
            val movies = model.movies.take(1).asSnapshot()
            assertEquals(30, movies.size)
            assertTrue(movies.all { it.title == feed.name })
            model.onEvent(MovieListEvent.MovieClicked(42))
            assertEquals(MovieListEffect.NavigateDetail(42), model.effectFlow.first())
            model.onEvent(MovieListEvent.BackClicked)
            assertEquals(MovieListEffect.NavigateBack, model.effectFlow.first())
        }
    }

    @Test fun invalidFeedCannotStartLoading() {
        for (feed in listOf("", "OTHER", "NOW_PLAYING")) {
            assertThrows(IllegalArgumentException::class.java) {
                MovieListViewModel(SavedStateHandle(mapOf("feed" to feed)), LoadHomeMoviesUseCase(repository))
            }
        }
    }
}
