package com.truongngo.moviedb.domain.auth

/** Local session information, not a replacement for server-side authorization. */
interface AuthSession {
    fun isSignedIn(): Boolean
    fun signOut()
}
