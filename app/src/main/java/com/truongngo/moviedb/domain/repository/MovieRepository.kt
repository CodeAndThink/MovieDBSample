package com.truongngo.moviedb.domain.repository

import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.model.HomeLoadMode
import com.truongngo.moviedb.domain.model.MovieLoadResult
import androidx.paging.PagingData
import com.truongngo.moviedb.domain.model.HomeMovie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun observeHomePreview(feed: HomeFeed): Flow<List<HomeMovie>>
    suspend fun refreshHomePreview(feed: HomeFeed, force: Boolean): com.truongngo.moviedb.domain.model.MovieLoadError?
    fun pagedHomeMovies(feed: HomeFeed): Flow<PagingData<HomeMovie>>
    fun loadHomeMovies(feed: HomeFeed, page: Int, mode: HomeLoadMode): Flow<MovieLoadResult>
}
