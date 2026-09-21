package com.truongngo.moviedb.domain.usecase

import com.truongngo.moviedb.domain.auth.RememberedEmailStore
import com.truongngo.moviedb.domain.model.AuthUser
import com.truongngo.moviedb.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LoginUseCaseTest {
    private var receivedEmail = ""
    private var receivedPassword = ""
    private var authFailure: Exception? = null
    private val repository = object : AuthRepository {
        override suspend fun login(email: String, password: String): Result<AuthUser> {
            receivedEmail = email
            receivedPassword = password
            return authFailure?.let { Result.failure(it) } ?: Result.success(AuthUser("1", email, null, false))
        }
        override suspend fun signUp(email: String, password: String): Result<AuthUser> = error("Unused")
    }
    private val store = MemoryStore()
    private val login = LoginUseCase(repository, store)

    @Test fun successfulLoginRemembersTrimmedEmailWithoutChangingPassword() = runTest {
        val user = login(" new@example.com ", " secret ", true)
        assertEquals("1", user.id)
        assertEquals("new@example.com", receivedEmail)
        assertEquals(" secret ", receivedPassword)
        assertEquals("new@example.com", store.email)
    }

    @Test fun successfulLoginWithoutRememberClearsExistingEmail() = runTest {
        login("new@example.com", "secret", false)
        assertNull(store.email)
    }

    @Test fun failedAuthenticationDoesNotChangeStoredEmail() = runTest {
        val failure = IllegalStateException("authentication failed")
        authFailure = failure
        try {
            login("new@example.com", "secret", true)
            fail("Expected auth failure")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
        assertEquals("saved@example.com", store.email)
    }

    @Test fun storageFailureDoesNotUndoAuthentication() = runTest {
        store.failure = IllegalStateException("storage unavailable")
        assertEquals("1", login("new@example.com", "secret", true).id)
        assertEquals("1", login("new@example.com", "secret", false).id)
    }

    @Test fun cancellationFromAuthenticationOrStoragePropagates() = runTest {
        val cancelled = CancellationException("cancelled")
        authFailure = cancelled
        try {
            login("new@example.com", "secret", true)
            fail("Expected auth cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
        assertEquals("saved@example.com", store.email)
        authFailure = null
        store.failure = cancelled
        try {
            login("new@example.com", "secret", true)
            fail("Expected storage cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }

    private class MemoryStore : RememberedEmailStore {
        var email: String? = "saved@example.com"
        var failure: Exception? = null
        override suspend fun read() = email
        override suspend fun save(email: String) { failure?.let { throw it }; this.email = email }
        override suspend fun clear() { failure?.let { throw it }; email = null }
    }
}
