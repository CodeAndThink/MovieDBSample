package com.truongngo.moviedb.presenter.signup

sealed interface SignupEffect {
    data object NavigateHome : SignupEffect
    data object NavigateLogin : SignupEffect
}
