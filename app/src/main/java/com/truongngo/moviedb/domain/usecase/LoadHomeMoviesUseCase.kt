package com.truongngo.moviedb.domain.usecase

import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.model.HomeLoadMode
import com.truongngo.moviedb.domain.model.MovieLoadResult
import com.truongngo.moviedb.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadHomeMoviesUseCase @Inject constructor(private val repository: MovieRepository) {
    operator fun invoke(feed: HomeFeed, page: Int = 1, mode: HomeLoadMode = HomeLoadMode.CACHE_FIRST): Flow<MovieLoadResult> {
        require(page in 1..500) { "Movie page must be between 1 and 500" }
        return repository.loadHomeMovies(feed, page, mode)
    }
}
