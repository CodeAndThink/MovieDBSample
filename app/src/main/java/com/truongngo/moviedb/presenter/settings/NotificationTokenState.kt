package com.truongngo.moviedb.presenter.settings

sealed interface NotificationTokenState {
    data object Idle : NotificationTokenState
    data object Loading : NotificationTokenState
    data class Ready(val token: String) : NotificationTokenState
    data object Error : NotificationTokenState
}
