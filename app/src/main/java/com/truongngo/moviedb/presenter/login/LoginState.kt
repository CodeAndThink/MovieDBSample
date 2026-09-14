package com.truongngo.moviedb.presenter.login

import com.truongngo.moviedb.presenter.enum.LoadStatus
import com.truongngo.moviedb.domain.model.AuthUser

data class LoginState(
    val user: AuthUser? = null,
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
    val loadStatus: LoadStatus = LoadStatus.INITIAL
)
