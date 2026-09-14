package com.truongngo.moviedb.data.network

import com.truongngo.moviedb.data.network.model.*
import com.truongngo.moviedb.data.network.utils.NetworkUtils
import javax.inject.Inject

class ApiClientsImpl @Inject constructor(private val service: TmdbService) : ApiClients {
    override suspend fun getPopularMovies(page: Int, language: String, region: String?): MoviePage {
        NetworkUtils.requirePage(page)
        return service.getPopularMovies(page, language, region)
    }

    override suspend fun getTopRatedMovies(page: Int, language: String, region: String?): MoviePage {
        NetworkUtils.requirePage(page)
        return service.getTopRatedMovies(page, language, region)
    }

    override suspend fun getNowPlayingMovies(page: Int, language: String, region: String?): MoviePage {
        NetworkUtils.requirePage(page)
        return service.getNowPlayingMovies(page, language, region)
    }

    override suspend fun getUpcomingMovies(page: Int, language: String, region: String?): MoviePage {
        NetworkUtils.requirePage(page)
        return service.getUpcomingMovies(page, language, region)
    }

    override suspend fun searchMovies(query: String, page: Int, language: String, includeAdult: Boolean): MoviePage {
        require(query.isNotBlank()) { "Search query must not be blank" }
        NetworkUtils.requirePage(page)
        return service.searchMovies(query, page, language, includeAdult)
    }

    override suspend fun getMovieDetails(movieId: Int, language: String): Movie {
        NetworkUtils.requireMovieId(movieId)
        return service.getMovieDetails(movieId, language)
    }

    override suspend fun getMovieVideos(movieId: Int, language: String): VideoResponse {
        NetworkUtils.requireMovieId(movieId)
        return service.getMovieVideos(movieId, language)
    }

    override suspend fun getMovieCredits(movieId: Int, language: String): CreditsResponse {
        NetworkUtils.requireMovieId(movieId)
        return service.getMovieCredits(movieId, language)
    }

    override suspend fun getMovieGenres(language: String): GenreResponse {
        return service.getMovieGenres(language)
    }

}
