package com.truongngo.moviedb.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.truongngo.moviedb.domain.auth.AuthSession
import javax.inject.Inject

class FirebaseAuthSession @Inject constructor(private val auth: FirebaseAuth) : AuthSession {
    override fun isSignedIn() = auth.currentUser != null
    override fun signOut() = auth.signOut()
}
