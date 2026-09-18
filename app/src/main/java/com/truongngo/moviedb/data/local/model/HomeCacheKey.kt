package com.truongngo.moviedb.data.local.model

/** Home currently requests en-US with no region. Keys include both for future callers. */
data class HomeCacheKey(val feed: String, val language: String = "en-US", val region: String = "")
