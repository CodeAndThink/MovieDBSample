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

// @Binds chỉ định implementation khi Hilt được yêu cầu cung cấp một contract ở tầng domain.
// Module nằm trong component cấp ứng dụng; các binding có @Singleton dùng chung một instance.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // Dùng FirebaseNotificationTokenProvider để lấy token thông báo theo contract của domain.
    @Binds
    abstract fun bindNotificationTokenProvider(implementation: FirebaseNotificationTokenProvider): NotificationTokenProvider


    @Binds
    @Singleton
    // Cung cấp MovieRepositoryImpl cho các nơi yêu cầu MovieRepository.
    abstract fun bindMovieRepository(implementation: MovieRepositoryImpl): MovieRepository


    @Binds
    @Singleton
    // Cung cấp implementation đọc/ghi cài đặt thông qua SettingsRepository.
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    // Cung cấp implementation xử lý xác thực thông qua AuthRepository.
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ) : AuthRepository
}
