package com.truongngo.moviedb.data.di

import android.content.Context
import androidx.room.Room
import com.truongngo.moviedb.data.local.home.HomeCache
import com.truongngo.moviedb.data.local.database.HomeCacheDatabase
import com.truongngo.moviedb.data.local.home.RoomHomeCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeCacheModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): HomeCacheDatabase =
        Room.databaseBuilder(context, HomeCacheDatabase::class.java, "home-cache.db").build()

    @Provides @Singleton
    fun cache(database: HomeCacheDatabase): HomeCache = RoomHomeCache(database.homeDao())
}
