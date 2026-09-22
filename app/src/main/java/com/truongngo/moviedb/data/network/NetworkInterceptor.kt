package com.truongngo.moviedb.data.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class NetworkInterceptor(
    private val accessToken: String,
    private val apiKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = onRequest(chain.request())
        return onResponse(chain.proceed(request))
    }

    private fun onRequest(original: Request): Request {
        validateOrigin(original)
        val token = accessToken.trim()
        val key = apiKey.trim()
        val url = original.url.newBuilder().removeAllQueryParameters("api_key")
        val request = original.newBuilder().header("Accept", "application/json")
            .removeHeader("Authorization")
        when {
            token.isNotEmpty() -> request.header("Authorization", "Bearer $token")
            key.isNotEmpty() -> url.addQueryParameter("api_key", key)
            else -> throw IOException("Missing MOVIEDB_ACCESS_TOKEN or MOVIEDB_API_KEY")
        }
        return request.url(url.build()).build()
    }

    private fun validateOrigin(request: Request) {
        // Never attach application credentials to an unrelated origin.
        if (request.url.scheme != "https" || request.url.host != "api.themoviedb.org" || request.url.port != 443) {
            throw IOException("TMDB client only supports https://api.themoviedb.org")
        }
    }

    private fun onResponse(response: Response): Response {
        if (response.isSuccessful) return response
        onError(response)
    }

    private fun onError(response: Response): Nothing {
        response.use {
            // Bound error reads and close failed responses, including malformed/HTML bodies.
            val body = runCatching { it.peekBody(64 * 1024L).string() }.getOrNull()
            throw NetworkErrorMapper.httpError(it.code, body, it.header("Retry-After"))
        }
    }
}
