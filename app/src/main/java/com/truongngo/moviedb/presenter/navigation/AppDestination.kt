package com.truongngo.moviedb.presenter.navigation

/** Typed routes; detail always carries a validated movie ID. */
sealed class AppDestination(val requiresAuthentication: Boolean) {
    data object LOGIN : AppDestination(false)
    data object SIGNUP : AppDestination(false)
    data object SEARCH : AppDestination(true)
    data object HOME : AppDestination(true)
    data object SETTINGS : AppDestination(true)
    data class Detail(val movieId: Int) : AppDestination(true) {
        init { require(movieId > 0) }
    }

    fun encode(): String = when (this) {
        LOGIN -> "LOGIN"
        SIGNUP -> "SIGNUP"
        SEARCH -> "SEARCH"
        HOME -> "HOME"
        SETTINGS -> "SETTINGS"
        is Detail -> "DETAIL:$movieId"
    }

    companion object {
        fun decode(value: String): AppDestination = when (value) {
            "LOGIN" -> LOGIN
            "SIGNUP" -> SIGNUP
            "SEARCH" -> SEARCH
            "HOME" -> HOME
            "SETTINGS" -> SETTINGS
            else -> {
                require(value.startsWith("DETAIL:")) { "Unknown route" }
                Detail(value.removePrefix("DETAIL:").toInt())
            }
        }
    }
}
