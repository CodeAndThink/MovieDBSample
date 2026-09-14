package com.truongngo.moviedb.data.network

import com.truongngo.moviedb.data.network.model.*

/** TMDB v3 movie APIs. HTTP failures throw NetworkException; transport failures throw IOException. */
interface ApiClients {
    suspend fun getPopularMovies(page: Int = 1, language: String = "en-US", region: String? = null): MoviePage
    suspend fun getTopRatedMovies(page: Int = 1, language: String = "en-US", region: String? = null): MoviePage
    suspend fun getNowPlayingMovies(page: Int = 1, language: String = "en-US", region: String? = null): MoviePage
    suspend fun getUpcomingMovies(page: Int = 1, language: String = "en-US", region: String? = null): MoviePage
    suspend fun searchMovies(query: String, page: Int = 1, language: String = "en-US", includeAdult: Boolean = false): MoviePage
    suspend fun getMovieDetails(movieId: Int, language: String = "en-US"): Movie
    suspend fun getMovieVideos(movieId: Int, language: String = "en-US"): VideoResponse
    suspend fun getMovieCredits(movieId: Int, language: String = "en-US"): CreditsResponse
    suspend fun getMovieGenres(language: String = "en-US"): GenreResponse
}
