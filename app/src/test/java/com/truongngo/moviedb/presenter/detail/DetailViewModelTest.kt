package com.truongngo.moviedb.presenter.detail

import androidx.lifecycle.SavedStateHandle
import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeApi()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun loadsMovieFromSavedArgumentAndBlocksDuplicateRequests() = runTest(dispatcher) {
        val pending = CompletableDeferred<Movie>()
        api.details = { pending.await() }
        val vm = DetailViewModel(api, SavedStateHandle(mapOf("movieId" to 42)))
        vm.onEvent(DetailEvent.Retry)
        runCurrent()
        assertTrue(vm.stateFlow.value.isLoading)
        assertEquals(listOf(42), api.detailCalls)
        pending.complete(page(1, listOf(42)).results.first())
        runCurrent()
        assertEquals(42, vm.stateFlow.value.movie?.id)
        assertFalse(vm.stateFlow.value.isLoading)
    }

    @Test fun retriesSameMovieAfterConnectionFailure() = runTest(dispatcher) {
        api.details = { throw IOException("offline") }
        val vm = DetailViewModel(api, SavedStateHandle(mapOf("movieId" to 7)))
        runCurrent()
        assertEquals(DetailError.CONNECTION, vm.stateFlow.value.error)
        api.details = { page(1, listOf(it)).results.first() }
        vm.onEvent(DetailEvent.Retry)
        runCurrent()
        assertEquals(listOf(7, 7), api.detailCalls)
        assertEquals(7, vm.stateFlow.value.movie?.id)
        assertNull(vm.stateFlow.value.error)
    }

    @Test fun missingMovieDoesNotCallApiAndBackEmitsEffect() = runTest(dispatcher) {
        val vm = DetailViewModel(api, SavedStateHandle())
        runCurrent()
        assertEquals(DetailError.INVALID_MOVIE, vm.stateFlow.value.error)
        assertTrue(api.detailCalls.isEmpty())
        vm.onEvent(DetailEvent.BackClicked)
        assertEquals(DetailEffect.NavigateBack, vm.effectFlow.first())
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
        override suspend fun searchMovies(query: String, page: Int, language: String, includeAdult: Boolean): MoviePage = error("Unused")
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
