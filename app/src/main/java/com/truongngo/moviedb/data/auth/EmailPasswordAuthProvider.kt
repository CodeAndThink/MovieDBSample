package com.truongngo.moviedb.data.auth

import com.truongngo.moviedb.domain.model.AuthUser

/**
 * Contract for email/password authentication services (Firebase, REST, etc.).
 * Implementations return domain identities and throw on failure; cancellation must propagate.
 * Other credential types should have their own contract rather than adding unused methods here.
 */
abstract class EmailPasswordAuthProvider {
    abstract suspend fun login(email: String, password: String): AuthUser
    abstract suspend fun signUp(email: String, password: String): AuthUser
}
