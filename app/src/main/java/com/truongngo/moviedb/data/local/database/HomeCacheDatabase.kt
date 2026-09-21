package com.truongngo.moviedb.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.truongngo.moviedb.data.local.database.dao.HomeCacheDao
import com.truongngo.moviedb.data.local.model.HomeMovieEntity
import com.truongngo.moviedb.data.local.model.HomePageEntity

@Database(entities = [HomePageEntity::class, HomeMovieEntity::class], version = 1, exportSchema = true)
abstract class HomeCacheDatabase : RoomDatabase() {
    abstract fun homeDao(): HomeCacheDao
}

