package com.truongngo.moviedb.domain.auth

/** Local email convenience only; never stores passwords or controls the auth session. */
interface RememberedEmailStore {
    suspend fun read(): String?
    suspend fun save(email: String)
    suspend fun clear()
}
