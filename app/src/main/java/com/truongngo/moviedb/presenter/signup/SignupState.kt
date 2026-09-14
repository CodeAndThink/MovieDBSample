package com.truongngo.moviedb.presenter.signup

import com.truongngo.moviedb.domain.auth.SignupException
import com.truongngo.moviedb.domain.model.AuthUser
import com.truongngo.moviedb.presenter.enum.LoadStatus

data class SignupState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
    val confirmPasswordError: Boolean = false,
    val error: SignupException.Reason? = null,
    val loadStatus: LoadStatus = LoadStatus.INITIAL,
    val user: AuthUser? = null
)
