package com.truongngo.moviedb.data.local.model

import com.truongngo.moviedb.data.network.model.MoviePage

data class CachedHomePage(val page: MoviePage, val fetchedAt: Long) {
    fun isFresh(now: Long): Boolean = now >= fetchedAt && now - fetchedAt < 30 * 60 * 1000L
}

