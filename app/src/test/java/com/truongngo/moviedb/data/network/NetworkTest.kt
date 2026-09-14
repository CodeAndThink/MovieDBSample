package com.truongngo.moviedb.data.network

import com.truongngo.moviedb.data.network.utils.NetworkUtils
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkTest {
    private fun client(token: String = "test-token", key: String = "test-key",
                       code: Int = 200, body: String = "{}",
                       inspect: (Request) -> Unit = {}): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(NetworkInterceptor(token, key))
        .addInterceptor { chain ->
            inspect(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("Test").header("Retry-After", "10")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()

    private fun execute(client: OkHttpClient, url: String = "https://api.themoviedb.org/3/movie/popular") {
        client.newCall(Request.Builder().url(url).build()).execute().close()
    }

    @Test fun bearerTakesPriorityAndRemovesQueryCredential() {
        execute(client { request ->
            assertEquals("Bearer test-token", request.header("Authorization"))
            assertEquals("application/json", request.header("Accept"))
            assertNull(request.url.queryParameter("api_key"))
        }, "https://api.themoviedb.org/3/movie/popular?api_key=old")
    }

    @Test fun fallsBackToApiKey() {
        execute(client(token = " ") { request ->
            assertNull(request.header("Authorization"))
            assertEquals("test-key", request.url.queryParameter("api_key"))
        })
    }

    @Test fun httpErrorExposesTmdbCodeAndRetryAfter() {
        val error = assertThrows(NetworkException::class.java) {
            execute(client(code = 429, body = """{"status_code":25,"status_message":"Rate limited"}"""))
        }
        assertEquals(429, error.httpCode)
        assertEquals(25, error.statusCode)
        assertEquals("Rate limited", error.statusMessage)
        assertEquals("10", error.retryAfter)
    }

    @Test fun malformedErrorStillPreservesHttpStatus() {
        val error = assertThrows(NetworkException::class.java) {
            execute(client(code = 502, body = "<html>Bad gateway</html>"))
        }
        assertEquals(502, error.httpCode)
        assertNull(error.statusCode)
    }

    @Test fun rejectsMissingCredentialsAndForeignHost() {
        assertThrows(java.io.IOException::class.java) { execute(client(token = "", key = "")) }
        assertThrows(java.io.IOException::class.java) { execute(client(), "https://example.com/") }
    }

    @Test fun retrofitSearchEncodesQueryAndDeserializesPage() = runBlocking {
        val http = client(body = """{"page":2,"total_pages":3,"total_results":41,"results":[{"id":11,"title":"Star Wars","poster_path":null,"vote_average":8.2,"vote_count":10}]}""") {
            assertEquals("/3/search/movie", it.url.encodedPath)
            assertEquals("Star Wars & friends", it.url.queryParameter("query"))
            assertEquals("2", it.url.queryParameter("page"))
            assertEquals("vi-VN", it.url.queryParameter("language"))
        }
        val service = Retrofit.Builder().baseUrl(NetworkUtils.BASE_URL).client(http)
            .addConverterFactory(GsonConverterFactory.create()).build().create(TmdbService::class.java)
        val api: ApiClients = ApiClientsImpl(service)
        val page = api.searchMovies("Star Wars & friends", page = 2, language = "vi-VN")
        assertEquals(3, page.totalPages)
        assertEquals("Star Wars", page.results.single().title)
        assertNull(page.results.single().posterPath)
        try {
            api.getPopularMovies(page = 0)
            fail("Invalid page must fail before a request")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun imageUrlHandlesMissingPaths() {
        assertNull(NetworkUtils.imageUrl(null))
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", NetworkUtils.imageUrl("/poster.jpg"))
    }
}
