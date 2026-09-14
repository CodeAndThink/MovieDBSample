package com.truongngo.moviedb.data.repository

import com.truongngo.moviedb.data.auth.EmailPasswordAuthProvider
import com.truongngo.moviedb.domain.model.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryImplTest {
    private val user = AuthUser("uid-123", "user@example.com", null, false)

    private fun provider(action: suspend (String, String) -> AuthUser) =
        object : EmailPasswordAuthProvider() {
            override suspend fun login(email: String, password: String) = action(email, password)
            override suspend fun signUp(email: String, password: String) = action(email, password)
        }

    @Test
    fun loginReturnsIdentityFromAlternativeProviderAndPreservesPassword() = runBlocking {
        val repository = AuthRepositoryImpl(provider { email, password ->
            assertEquals("user@example.com", email)
            assertEquals(" password ", password)
            user
        })
        assertEquals(user, repository.login("user@example.com", " password ").getOrThrow())
    }

    @Test
    fun providerFailureBecomesFailedResult() = runBlocking {
        val failure = IllegalArgumentException("Invalid credentials")
        val repository = AuthRepositoryImpl(provider { _, _ -> throw failure })
        assertSame(failure, repository.login("user@example.com", "incorrect").exceptionOrNull())
    }

    @Test
    fun cancellationIsNotConvertedIntoLoginFailure() = runBlocking {
        val cancellation = CancellationException("Cancelled")
        val repository = AuthRepositoryImpl(provider { _, _ -> throw cancellation })
        try {
            repository.login("user@example.com", "password")
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun signupUsesRegistrationInsteadOfLogin() = runBlocking {
        val repository = AuthRepositoryImpl(object : EmailPasswordAuthProvider() {
            override suspend fun login(email: String, password: String): AuthUser =
                error("Registration must not call login")
            override suspend fun signUp(email: String, password: String) = user
        })
        assertEquals(user, repository.signUp("user@example.com", "password").getOrThrow())
    }
}
