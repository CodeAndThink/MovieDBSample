package com.truongngo.moviedb.data.network.utils

import com.google.gson.JsonParser
import com.truongngo.moviedb.data.network.NetworkException

object NetworkUtils {
    const val BASE_URL = "https://api.themoviedb.org/3/"
    const val TIMEOUT_SECONDS = 30L

    fun requirePage(page: Int) {
        require(page in 1..500) { "Page must be between 1 and 500" }
    }

    fun requireMovieId(movieId: Int) {
        require(movieId > 0) { "Movie ID must be positive" }
    }

    /** A null/empty poster or backdrop path means that TMDB has no image. */
    fun imageUrl(path: String?, size: String = "w500"): String? {
        if (path.isNullOrBlank()) return null
        require(size == "original" || size.matches(Regex("[wh][1-9][0-9]*"))) { "Invalid image size" }
        return "https://image.tmdb.org/t/p/$size/${path.trimStart('/')}"
    }

    fun httpError(code: Int, body: String?, retryAfter: String? = null): NetworkException {
        val json = runCatching { JsonParser.parseString(body.orEmpty()).asJsonObject }.getOrNull()
        val status = runCatching { json?.get("status_code")?.asInt }.getOrNull()
        val message = runCatching { json?.get("status_message")?.asString }.getOrNull()
        return NetworkException(code, status, message, retryAfter)
    }
}
