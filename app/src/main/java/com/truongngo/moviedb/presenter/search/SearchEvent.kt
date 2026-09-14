package com.truongngo.moviedb.presenter.search

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class MovieClicked(val movieId: Int) : SearchEvent
    data object Submit : SearchEvent
    data object Retry : SearchEvent
    data object LoadMore : SearchEvent
    data object BackClicked : SearchEvent
}
