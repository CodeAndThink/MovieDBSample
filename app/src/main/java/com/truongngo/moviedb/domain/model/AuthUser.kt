package com.truongngo.moviedb.domain.model

/** Provider-independent identity. Passwords and tokens must not be stored in UI state. */
data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean
)
