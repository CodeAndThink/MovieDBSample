package com.truongngo.moviedb.domain.model

/** Safe UI-facing paging failure, without network implementation details or credentials. */
class MoviePagingException(val reason: MovieLoadError) : Exception(reason.name)
