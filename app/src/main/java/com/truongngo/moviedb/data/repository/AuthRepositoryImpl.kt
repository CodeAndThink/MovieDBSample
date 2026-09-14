package com.truongngo.moviedb.data.repository

import com.truongngo.moviedb.data.auth.EmailPasswordAuthProvider
import com.truongngo.moviedb.domain.model.AuthUser
import com.truongngo.moviedb.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val provider: EmailPasswordAuthProvider
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<AuthUser> =
        authResult { provider.login(email, password) }

    override suspend fun signUp(email: String, password: String): Result<AuthUser> =
        authResult { provider.signUp(email, password) }

    private suspend fun authResult(action: suspend () -> AuthUser): Result<AuthUser> = try {
        Result.success(action())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
