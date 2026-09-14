package com.truongngo.moviedb.domain.repository

import com.truongngo.moviedb.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observe(movieId: Int): Flow<DownloadState>
    suspend fun start(movieId: Int, title: String)
    suspend fun cancel(movieId: Int)
}
