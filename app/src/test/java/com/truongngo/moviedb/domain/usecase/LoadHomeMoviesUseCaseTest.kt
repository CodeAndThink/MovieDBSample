package com.truongngo.moviedb.domain.usecase

import com.truongngo.moviedb.domain.model.*
import com.truongngo.moviedb.domain.repository.MovieRepository
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class LoadHomeMoviesUseCaseTest {
    @Test fun invalidPagesAreRejectedBeforeStartingDataWork() {
        var calls = 0
        val repository = object : MovieRepository {
            override fun loadHomeMovies(feed: HomeFeed, page: Int, mode: HomeLoadMode) =
                flowOf<MovieLoadResult>(MovieLoadResult.Data(HomeMoviePage(page, emptyList(), 1, 0))).also { calls++ }
        }
        val load = LoadHomeMoviesUseCase(repository)
        for (page in listOf(-1, 0, 501)) {
            try {
                load(HomeFeed.POPULAR, page)
                fail("Expected invalid page to be rejected: $page")
            } catch (_: IllegalArgumentException) {
                // Invalid page must not reach the repository.
            }
        }
        assertEquals(0, calls)
    }
}
