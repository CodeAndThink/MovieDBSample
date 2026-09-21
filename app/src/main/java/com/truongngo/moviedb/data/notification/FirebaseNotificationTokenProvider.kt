package com.truongngo.moviedb.data.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.truongngo.moviedb.domain.repository.NotificationTokenProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseNotificationTokenProvider @Inject constructor() : NotificationTokenProvider {
    // Keep the existing registration-token mode for Firebase Console device tests.
    // Do not opt into FID registration while this screen exposes a legacy FCM token.
    @Suppress("DEPRECATION")
    override suspend fun getCurrentToken(): String = FirebaseMessaging.getInstance().token.await()
}
