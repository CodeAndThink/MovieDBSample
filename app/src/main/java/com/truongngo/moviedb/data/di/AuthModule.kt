package com.truongngo.moviedb.data.di

import com.google.firebase.auth.FirebaseAuth
import com.truongngo.moviedb.data.auth.EmailPasswordAuthProvider
import com.truongngo.moviedb.data.auth.FirebaseEmailPasswordAuthProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindRememberedEmailStore(
        impl: com.truongngo.moviedb.data.local.EncryptedRememberedEmailStore
    ): com.truongngo.moviedb.domain.auth.RememberedEmailStore

    @Binds
    @Singleton
    abstract fun bindAuthSession(impl: com.truongngo.moviedb.data.auth.FirebaseAuthSession): com.truongngo.moviedb.domain.auth.AuthSession

    @Binds
    @Singleton
    abstract fun bindEmailPasswordAuthProvider(
        provider: FirebaseEmailPasswordAuthProvider
    ): EmailPasswordAuthProvider

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
