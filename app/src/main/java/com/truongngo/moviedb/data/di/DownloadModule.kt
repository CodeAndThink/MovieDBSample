package com.truongngo.moviedb.data.di

import com.truongngo.moviedb.data.repository.DownloadRepositoryImpl
import com.truongngo.moviedb.domain.repository.DownloadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadModule {
    @Binds abstract fun bindDownloadRepository(implementation: DownloadRepositoryImpl): DownloadRepository
}
