package com.truongngo.moviedb.data.local.home

import com.truongngo.moviedb.data.local.model.CachedHomePage
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.network.model.MoviePage

interface HomeCache {
    suspend fun read(key: HomeCacheKey): CachedHomePage?
    suspend fun write(key: HomeCacheKey, page: MoviePage)
}
