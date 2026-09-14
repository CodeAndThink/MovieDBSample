package com.truongngo.moviedb.data.network

import java.io.IOException

/** An HTTP error. Transport errors retain their original IOException subtype. */
class NetworkException(
    val httpCode: Int,
    val statusCode: Int?,
    val statusMessage: String?,
    val retryAfter: String? = null,
) : IOException("TMDB request failed (HTTP $httpCode)")
