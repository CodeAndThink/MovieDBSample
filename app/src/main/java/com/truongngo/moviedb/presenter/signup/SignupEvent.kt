package com.truongngo.moviedb.presenter.signup

sealed interface SignupEvent {
    data class EmailChanged(val value: String) : SignupEvent
    data class PasswordChanged(val value: String) : SignupEvent
    data class ConfirmPasswordChanged(val value: String) : SignupEvent
    data object SignupClicked : SignupEvent
    data object LoginClicked : SignupEvent
}
