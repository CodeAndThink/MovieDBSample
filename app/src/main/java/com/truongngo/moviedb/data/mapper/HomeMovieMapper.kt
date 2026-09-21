package com.truongngo.moviedb.data.mapper

import com.truongngo.moviedb.data.network.model.Movie
import com.truongngo.moviedb.data.network.model.MoviePage
import com.truongngo.moviedb.domain.model.HomeMovie
import com.truongngo.moviedb.domain.model.HomeMoviePage

fun Movie.toHomeMovie() = HomeMovie(id, title, posterPath, backdropPath, releaseDate, voteAverage)

fun MoviePage.toHomeMoviePage() = HomeMoviePage(page, results.map { it.toHomeMovie() }, totalPages, totalResults)
