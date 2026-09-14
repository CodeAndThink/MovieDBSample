package com.truongngo.moviedb.data.network

import com.truongngo.moviedb.data.network.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {
    @GET("movie/popular")
    suspend fun getPopularMovies(@Query("page") page: Int, @Query("language") language: String, @Query("region") region: String?): MoviePage

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(@Query("page") page: Int, @Query("language") language: String, @Query("region") region: String?): MoviePage

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(@Query("page") page: Int, @Query("language") language: String, @Query("region") region: String?): MoviePage

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(@Query("page") page: Int, @Query("language") language: String, @Query("region") region: String?): MoviePage

    @GET("search/movie")
    suspend fun searchMovies(@Query("query") query: String, @Query("page") page: Int, @Query("language") language: String, @Query("include_adult") includeAdult: Boolean): MoviePage

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(@Path("movie_id") movieId: Int, @Query("language") language: String): Movie

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(@Path("movie_id") movieId: Int, @Query("language") language: String): VideoResponse

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(@Path("movie_id") movieId: Int, @Query("language") language: String): CreditsResponse

    @GET("genre/movie/list")
    suspend fun getMovieGenres(@Query("language") language: String): GenreResponse

}
