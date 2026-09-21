package com.truongngo.moviedb

import android.app.Application
import com.truongngo.moviedb.presenter.notification.PushNotificationHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PracticeApplication : Application() {
    @Inject lateinit var pushNotifications: PushNotificationHandler

    override fun onCreate() {
        super.onCreate()
        pushNotifications.createChannel()
    }
}
