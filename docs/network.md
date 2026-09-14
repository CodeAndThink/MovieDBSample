# TMDB network

Credentials are read from the root `.env` at build time. Environment variables with
matching names take precedence, for CI. Rebuild after changing credentials.
`MOVIEDB_ACCESS_TOKEN` is sent as a Bearer header; if blank,
`MOVIEDB_API_KEY` is sent as the `api_key` query parameter. Both blank causes a
local IOException before sending a request. No credential logging is installed.
`.env` is ignored by Git; BuildConfig values are still embedded in the APK.

Inject `ApiClients` through Hilt and call its suspend functions from a coroutine:

```kotlin
class MovieRepository @Inject constructor(private val api: ApiClients) {
    suspend fun popular(page: Int = 1) =
        api.getPopularMovies(page = page, language = "vi-VN", region = "VN")

    suspend fun details(id: Int) = api.getMovieDetails(id, language = "vi-VN")
}
```

Supported methods: `getPopularMovies`, `getTopRatedMovies`, `getNowPlayingMovies`,
`getUpcomingMovies`, `searchMovies`, `getMovieDetails`, `getMovieVideos`,
`getMovieCredits`, and `getMovieGenres`. This covers movie browsing, not the full
TMDB catalogue of TV, account, session, and write endpoints. Movie list responses
include pagination metadata; `Movie.genres`/`runtime` are detail-only fields,
while `genreIds` belongs to list/search responses.

`NetworkException` extends IOException and exposes `httpCode`, TMDB `statusCode`,
`statusMessage`, and the raw `Retry-After` header. Non-JSON HTTP errors retain the
HTTP code. Offline and timeout failures retain their IOException subtype.
Catch NetworkException before IOException if presenting different UI states.
Coroutine cancellation is left to Retrofit and is not wrapped or swallowed.
There is no automatic retry of HTTP failures. Redirects are disabled to keep
credentials on the expected TMDB origin.

`NetworkUtils.imageUrl(movie.posterPath)` returns an image URL or null when no
image exists; sizes should come from TMDB's supported image configuration.

References:
- https://developer.themoviedb.org/docs/authentication-application
- https://developer.themoviedb.org/reference/movie-popular-list
- https://developer.themoviedb.org/reference/search-movie
- https://developer.themoviedb.org/reference/movie-details

Validation: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
Network tests use synthetic responses, not real credentials or live TMDB calls.
