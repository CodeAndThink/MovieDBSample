package com.truongngo.moviedb.presenter.notification

data class PushNotificationPayload(
    val id: String,
    val title: String?,
    val body: String?,
    val deepLink: String?,
)
