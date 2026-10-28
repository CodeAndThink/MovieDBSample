package com.truongngo.moviedb.data.di

import com.truongngo.moviedb.data.repository.DownloadRepositoryImpl
import com.truongngo.moviedb.domain.repository.DownloadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Đăng ký ánh xạ dependency tải phim trong component cấp ứng dụng của Hilt.
@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadModule {
    // Khi cần DownloadRepository, Hilt cung cấp DownloadRepositoryImpl qua @Binds.
    // Scope @Singleton được khai báo trên DownloadRepositoryImpl.
    @Binds abstract fun bindDownloadRepository(implementation: DownloadRepositoryImpl): DownloadRepository
}
