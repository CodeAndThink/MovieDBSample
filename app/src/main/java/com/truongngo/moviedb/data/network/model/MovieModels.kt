package com.truongngo.moviedb.data.network.model

import com.google.gson.annotations.SerializedName

data class MoviePage(
    val page: Int,
    val results: List<Movie>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int,
)

data class Movie(
    val id: Int,
    val title: String,
    val overview: String?,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    val genres: List<Genre>?,
    val runtime: Int?,
)

data class Genre(val id: Int, val name: String)
data class GenreResponse(val genres: List<Genre>)
data class VideoResponse(val id: Int, val results: List<Video>)
data class Video(val id: String, val key: String, val name: String, val site: String, val type: String)
data class CreditsResponse(val id: Int, val cast: List<CastMember>, val crew: List<CrewMember>)
data class CastMember(val id: Int, val name: String, val character: String?,
    @SerializedName("profile_path") val profilePath: String?)
data class CrewMember(val id: Int, val name: String, val job: String?, val department: String?)
