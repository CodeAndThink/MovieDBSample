package com.truongngo.moviedb.presenter.search

import androidx.lifecycle.SavedStateHandle
import com.truongngo.moviedb.domain.model.MovieLoadError
import com.truongngo.moviedb.data.network.NetworkException
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeApi()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun debounceWaits500msAndOnlySearchesLatestText() = runTest(dispatcher) {
        val vm = SearchViewModel(api, SavedStateHandle())
        vm.onEvent(SearchEvent.QueryChanged("bat"))
        advanceTimeBy(400)
        vm.onEvent(SearchEvent.QueryChanged("batman"))
        advanceTimeBy(499)
        runCurrent()
        assertTrue(api.calls.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("batman" to 1), api.calls)
        assertEquals(1, vm.stateFlow.value.movies.size)
    }

    @Test fun clearCancelsPendingSearch() = runTest(dispatcher) {
        val vm = SearchViewModel(api, SavedStateHandle())
        vm.onEvent(SearchEvent.QueryChanged("batman"))
        advanceTimeBy(200)
        vm.onEvent(SearchEvent.QueryChanged(""))
        advanceUntilIdle()
        assertTrue(api.calls.isEmpty())
        assertFalse(vm.stateFlow.value.isLoading)
        assertTrue(vm.stateFlow.value.movies.isEmpty())
    }

    @Test fun staleResponseCannotReplaceNewQuery() = runTest(dispatcher) {
        val pending = CompletableDeferred<MoviePage>()
        api.search = { q, p -> if (q == "old") withContext(NonCancellable) { pending.await() } else page(p, listOf(2)) }
        val vm = SearchViewModel(api, SavedStateHandle())
        vm.onEvent(SearchEvent.QueryChanged("old"))
        advanceTimeBy(500)
        runCurrent()
        vm.onEvent(SearchEvent.QueryChanged("new"))
        advanceTimeBy(500)
        runCurrent()
        pending.complete(page(1, listOf(1)))
        runCurrent()
        assertEquals(listOf(2), vm.stateFlow.value.movies.map { it.id })
    }

    @Test fun failedPageRetriesAndKeepsExistingResults() = runTest(dispatcher) {
        val vm = SearchViewModel(api, SavedStateHandle(mapOf("query" to "movie")))
        advanceUntilIdle()
        api.search = { _, _ -> throw IOException("offline") }
        vm.onEvent(SearchEvent.LoadMore)
        runCurrent()
        assertEquals(MovieLoadError.CONNECTION, vm.stateFlow.value.error)
        assertEquals(1, vm.stateFlow.value.movies.size)
        api.search = { _, p -> page(p, listOf(1, 2)) }
        vm.onEvent(SearchEvent.Retry)
        runCurrent()
        assertEquals(listOf("movie" to 1, "movie" to 2, "movie" to 2), api.calls)
        assertEquals(listOf(1, 2), vm.stateFlow.value.movies.map { it.id })
        assertNull(vm.stateFlow.value.error)
    }

    @Test fun httpFailureKeepsItsCategoryInState() = runTest(dispatcher) {
        api.search = { _, _ -> throw NetworkException(500, null, null) }
        val vm = SearchViewModel(api, SavedStateHandle(mapOf("query" to "movie")))
        advanceUntilIdle()
        assertEquals(MovieLoadError.SERVER, vm.stateFlow.value.error)
    }

    @Test fun staleFailureCannotReplaceSuccessfulNewQuery() = runTest(dispatcher) {
        val pending = CompletableDeferred<MoviePage>()
        api.search = { q, p -> if (q == "old") withContext(NonCancellable) { pending.await() } else page(p, listOf(2)) }
        val vm = SearchViewModel(api, SavedStateHandle(mapOf("query" to "old")))
        advanceTimeBy(500)
        runCurrent()
        vm.onEvent(SearchEvent.QueryChanged("new"))
        advanceTimeBy(500)
        runCurrent()
        pending.completeExceptionally(NetworkException(500, null, null))
        runCurrent()
        assertEquals(listOf(2), vm.stateFlow.value.movies.map { it.id })
        assertNull(vm.stateFlow.value.error)
        assertFalse(vm.stateFlow.value.isLoading)
    }

    private class FakeApi : ApiClients {
        val popularCalls = mutableListOf<Int>()
        var popular: suspend (Int) -> MoviePage = { page(it, listOf(1, 2)) }
        var topRated: suspend (Int) -> MoviePage = { page(it, listOf(3, 4)) }
        override suspend fun getPopularMovies(page: Int, language: String, region: String?): MoviePage {
            popularCalls += page
            return popular(page)
        }
        override suspend fun getTopRatedMovies(page: Int, language: String, region: String?) = topRated(page)
        override suspend fun getUpcomingMovies(page: Int, language: String, region: String?) = page(page, listOf(5, 6))
        override suspend fun getNowPlayingMovies(page: Int, language: String, region: String?) = page(page, (1..20).toList())
        val calls = mutableListOf<Pair<String, Int>>()
        var search: suspend (String, Int) -> MoviePage = { _, p -> page(p, listOf(p)) }
        override suspend fun searchMovies(query: String, page: Int, language: String, includeAdult: Boolean): MoviePage {
            calls += query to page
            return search(query, page)
        }
        val detailCalls = mutableListOf<Int>()
        var details: suspend (Int) -> Movie = { page(1, listOf(it)).results.first() }
        override suspend fun getMovieDetails(movieId: Int, language: String): Movie {
            detailCalls += movieId
            return details(movieId)
        }
        override suspend fun getMovieVideos(movieId: Int, language: String): VideoResponse = error("Unused")
        override suspend fun getMovieCredits(movieId: Int, language: String): CreditsResponse = error("Unused")
        override suspend fun getMovieGenres(language: String): GenreResponse = error("Unused")
    }

    companion object {
        private fun page(page: Int, ids: List<Int>, total: Int = 3) = MoviePage(page,
            ids.map { Movie(it, "Movie $it", null, null, null, null, "2026-01-01", 8.0, 10, null, null, null) }, total, 60)
    }
}
