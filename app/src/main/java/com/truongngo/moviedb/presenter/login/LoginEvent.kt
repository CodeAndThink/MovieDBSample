package com.truongngo.moviedb.presenter.login

sealed interface LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent
    data class PasswordChanged(val password: String) : LoginEvent
    data class RememberMeChanged(val checked: Boolean) : LoginEvent
    data object LoginClicked : LoginEvent
    data object SignupClicked : LoginEvent
}
