package com.truongngo.moviedb.data.di

import com.truongngo.moviedb.data.local.database.dao.HomePagingDao
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

// Cung cấp database và cache cho Home; @Singleton dùng chung instance trong component cấp ứng dụng.
@Module
@InstallIn(SingletonComponent::class)
object HomeCacheModule {
    @Provides
    fun pagingDao(database: HomeCacheDatabase): HomePagingDao = database.pagingDao()

    // databaseBuilder cấu hình Room với Context ứng dụng và file "home-cache.db".
    // build() tạo instance database để các dependency khác truy cập dữ liệu cache.
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): HomeCacheDatabase =
        Room.databaseBuilder(context, HomeCacheDatabase::class.java, "home-cache.db").build()

    // homeDao() lấy DAO truy cập dữ liệu; RoomHomeCache bọc DAO để đáp ứng contract HomeCache.
    @Provides @Singleton
    fun cache(database: HomeCacheDatabase): HomeCache = RoomHomeCache(database.homeDao())
}
