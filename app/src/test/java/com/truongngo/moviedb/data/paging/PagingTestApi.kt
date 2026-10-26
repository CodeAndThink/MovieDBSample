package com.truongngo.moviedb.data.paging

import com.truongngo.moviedb.data.network.ApiClients
import com.truongngo.moviedb.data.network.model.*

internal class PagingTestApi : ApiClients {
    val popularCalls = mutableListOf<Int>()
    val calls = mutableListOf<String>()
    var nowPlaying: suspend () -> MoviePage = { moviePage(1, (1..20).toList()) }
    var popular: suspend (Int) -> MoviePage = { moviePage(it, listOf(1, 2)) }
    var topRated: suspend (Int) -> MoviePage = { moviePage(it, listOf(3, 4)) }
    override suspend fun getPopularMovies(page: Int, language: String, region: String?): MoviePage {
        calls += "POPULAR"
        popularCalls += page
        return popular(page)
    }
    override suspend fun getTopRatedMovies(page: Int, language: String, region: String?): MoviePage {
        calls += "TOP_RATED"
        return topRated(page)
    }
    override suspend fun getUpcomingMovies(page: Int, language: String, region: String?): MoviePage {
        calls += "UPCOMING"
        return moviePage(page, listOf(5, 6))
    }
    override suspend fun getNowPlayingMovies(page: Int, language: String, region: String?): MoviePage {
        calls += "NOW_PLAYING"
        return nowPlaying()
    }
    override suspend fun searchMovies(query: String, page: Int, language: String, includeAdult: Boolean): MoviePage = error("Unused")
    override suspend fun getMovieDetails(movieId: Int, language: String): Movie = error("Unused")
    override suspend fun getMovieVideos(movieId: Int, language: String): VideoResponse = error("Unused")
    override suspend fun getMovieCredits(movieId: Int, language: String): CreditsResponse = error("Unused")
    override suspend fun getMovieGenres(language: String): GenreResponse = error("Unused")
}

internal fun moviePage(page: Int, ids: List<Int>, total: Int = 3) = MoviePage(page,
        ids.map { Movie(it, "Movie $it", null, null, null, null, "2026-01-01", 8.0, 10, null, null, null) }, total, 60)
