package com.truongngo.moviedb.data.network

import com.google.gson.JsonParser
import com.truongngo.moviedb.domain.model.MovieLoadError
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

object NetworkErrorMapper {
    fun httpError(code: Int, body: String?, retryAfter: String? = null): NetworkException {
        val json = runCatching { JsonParser.parseString(body.orEmpty()).asJsonObject }.getOrNull()
        val status = runCatching { json?.get("status_code")?.asInt }.getOrNull()
        val message = runCatching { json?.get("status_message")?.asString }.getOrNull()
        return NetworkException(code, status, message, retryAfter)
    }

    fun toMovieLoadError(error: Throwable): MovieLoadError = when (error) {
        is CancellationException -> throw error
        // HTTP exceptions inherit IOException, so classify them first.
        is NetworkException -> when (error.httpCode) {
            401 -> MovieLoadError.AUTHENTICATION
            403 -> MovieLoadError.FORBIDDEN
            404 -> MovieLoadError.NOT_FOUND
            429 -> MovieLoadError.RATE_LIMITED
            in 500..599 -> MovieLoadError.SERVER
            else -> MovieLoadError.GENERAL
        }
        is SocketTimeoutException -> MovieLoadError.TIMEOUT
        // OkHttp callTimeout uses InterruptedIOException("timeout"), unlike socket timeouts.
        is InterruptedIOException -> if (error.message == "timeout") MovieLoadError.TIMEOUT else MovieLoadError.CONNECTION
        is IOException -> MovieLoadError.CONNECTION
        else -> MovieLoadError.GENERAL
    }
}
