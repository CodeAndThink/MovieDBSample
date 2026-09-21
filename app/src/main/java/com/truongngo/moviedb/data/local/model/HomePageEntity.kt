package com.truongngo.moviedb.data.local.model

import androidx.room.Embedded
import androidx.room.Entity

@Entity(tableName = "home_pages", primaryKeys = ["feed", "language", "region"])
data class HomePageEntity(
    @Embedded val key: HomeCacheKey,
    val totalPages: Int,
    val totalResults: Int,
    val fetchedAt: Long,
)

