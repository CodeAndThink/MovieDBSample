package com.truongngo.moviedb.domain.repository

fun interface NotificationTokenProvider {
    suspend fun getCurrentToken(): String
}
