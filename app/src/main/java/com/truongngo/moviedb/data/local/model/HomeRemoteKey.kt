package com.truongngo.moviedb.data.local.model

import androidx.room.Embedded
import androidx.room.Entity

@Entity(tableName = "home_remote_keys", primaryKeys = ["feed", "language", "region"])
data class HomeRemoteKey(
    @Embedded val key: HomeCacheKey,
    val nextPage: Int?,
    val refreshedAt: Long,
    val generation: String,
)
