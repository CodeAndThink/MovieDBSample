package com.truongngo.moviedb.data.di

import com.truongngo.moviedb.data.repository.MovieRepositoryImpl
import com.truongngo.moviedb.domain.repository.MovieRepository
import com.truongngo.moviedb.data.repository.AuthRepositoryImpl
import com.truongngo.moviedb.data.repository.SettingsRepositoryImpl
import com.truongngo.moviedb.domain.repository.AuthRepository
import com.truongngo.moviedb.domain.repository.SettingsRepository
import com.truongngo.moviedb.data.notification.FirebaseNotificationTokenProvider
import com.truongngo.moviedb.domain.repository.NotificationTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindNotificationTokenProvider(implementation: FirebaseNotificationTokenProvider): NotificationTokenProvider


    @Binds
    @Singleton
    abstract fun bindMovieRepository(implementation: MovieRepositoryImpl): MovieRepository


    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ) : AuthRepository
}
