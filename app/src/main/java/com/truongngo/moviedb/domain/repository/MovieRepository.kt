package com.truongngo.moviedb.domain.repository

import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.model.HomeLoadMode
import com.truongngo.moviedb.domain.model.MovieLoadResult
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun loadHomeMovies(feed: HomeFeed, page: Int, mode: HomeLoadMode): Flow<MovieLoadResult>
}
