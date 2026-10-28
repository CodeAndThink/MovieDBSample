package com.truongngo.moviedb.data.local.database

import androidx.room.AutoMigration
import com.truongngo.moviedb.data.local.database.dao.HomePagingDao
import com.truongngo.moviedb.data.local.model.HomeFeedMovieEntity
import com.truongngo.moviedb.data.local.model.HomeRemoteKey
import androidx.room.Database
import androidx.room.RoomDatabase
import com.truongngo.moviedb.data.local.database.dao.HomeCacheDao
import com.truongngo.moviedb.data.local.model.HomeMovieEntity
import com.truongngo.moviedb.data.local.model.HomePageEntity

@Database(entities = [HomePageEntity::class, HomeMovieEntity::class, HomeFeedMovieEntity::class, HomeRemoteKey::class],
    autoMigrations = [AutoMigration(from = 1, to = 2)], version = 2, exportSchema = true)
abstract class HomeCacheDatabase : RoomDatabase() {
    abstract fun pagingDao(): HomePagingDao
    abstract fun homeDao(): HomeCacheDao
}

