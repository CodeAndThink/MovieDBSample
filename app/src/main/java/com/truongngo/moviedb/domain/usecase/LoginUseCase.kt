package com.truongngo.moviedb.domain.usecase

import com.truongngo.moviedb.domain.auth.RememberedEmailStore
import com.truongngo.moviedb.domain.model.AuthUser
import com.truongngo.moviedb.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val rememberedEmail: RememberedEmailStore,
) {
    suspend operator fun invoke(email: String, password: String, rememberEmail: Boolean): AuthUser {
        val normalizedEmail = email.trim()
        val user = repository.login(normalizedEmail, password).getOrThrow()
        currentCoroutineContext().ensureActive()
        try {
            if (rememberEmail) rememberedEmail.save(normalizedEmail) else rememberedEmail.clear()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Remembering an email is optional; storage failure must not undo authentication.
        }
        return user
    }
}
