package com.truongngo.moviedb.data.local.home

import com.truongngo.moviedb.data.local.database.dao.HomeCacheDao
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.network.model.MoviePage

class RoomHomeCache(private val dao: HomeCacheDao) : HomeCache {
    override suspend fun read(key: HomeCacheKey) = dao.read(key)
    override suspend fun write(key: HomeCacheKey, page: MoviePage) = dao.replace(key, page, System.currentTimeMillis())
}
