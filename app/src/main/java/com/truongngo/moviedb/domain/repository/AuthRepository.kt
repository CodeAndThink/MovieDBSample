package com.truongngo.moviedb.domain.repository

import com.truongngo.moviedb.domain.model.AuthUser

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthUser>

    suspend fun signUp(email: String, password: String): Result<AuthUser>
}