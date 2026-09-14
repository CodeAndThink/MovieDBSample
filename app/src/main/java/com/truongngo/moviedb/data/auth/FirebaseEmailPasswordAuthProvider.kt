package com.truongngo.moviedb.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.truongngo.moviedb.domain.auth.SignupException
import kotlinx.coroutines.CancellationException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.truongngo.moviedb.domain.model.AuthUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseEmailPasswordAuthProvider @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : EmailPasswordAuthProvider() {
    override suspend fun login(email: String, password: String): AuthUser =
        requireNotNull(firebaseAuth.signInWithEmailAndPassword(email, password).await().user) {
            "Authentication completed without a user"
        }.toDomain()

    override suspend fun signUp(email: String, password: String): AuthUser = try {
        requireNotNull(firebaseAuth.createUserWithEmailAndPassword(email, password).await().user) {
            "Registration completed without a user"
        }.toDomain()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        val reason = when (exception) {
            is FirebaseAuthWeakPasswordException -> SignupException.Reason.WEAK_PASSWORD
            is FirebaseAuthUserCollisionException -> SignupException.Reason.EMAIL_IN_USE
            is FirebaseAuthInvalidCredentialsException -> SignupException.Reason.INVALID_EMAIL
            is FirebaseNetworkException -> SignupException.Reason.NETWORK
            is FirebaseTooManyRequestsException -> SignupException.Reason.TOO_MANY_REQUESTS
            else -> SignupException.Reason.UNKNOWN
        }
        throw SignupException(reason, exception)
    }

    private fun FirebaseUser.toDomain() = AuthUser(
        id = uid,
        email = email,
        displayName = displayName,
        isEmailVerified = isEmailVerified
    )
}
