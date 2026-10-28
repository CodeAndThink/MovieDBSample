package com.truongngo.moviedb.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index

/** Each feed owns its ordered, deduplicated rows independently of the banner snapshot. */
@Entity(tableName = "home_feed_movies", primaryKeys = ["feed", "language", "region", "movieId"],
    indices = [Index(value = ["feed", "language", "region", "position"])])
data class HomeFeedMovieEntity(@Embedded val movie: HomeMovieEntity)
