package com.truongngo.moviedb.presenter.login

sealed interface LoginEffect {
    data object ShowLoginError : LoginEffect
    data object NavigateHome : LoginEffect
    data object NavigateSignup : LoginEffect
}
