package com.truongngo.moviedb.data.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.truongngo.moviedb.presenter.notification.PushNotificationHandler
import com.truongngo.moviedb.presenter.notification.PushNotificationPayload
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var handler: PushNotificationHandler

    override fun onMessageReceived(message: RemoteMessage) {
        handler.show(PushNotificationPayload(
            id = message.messageId ?: UUID.randomUUID().toString(),
            title = message.notification?.title ?: message.data["title"],
            body = message.notification?.body ?: message.data["body"],
            deepLink = message.data["deep_link"],
        ))
    }

    // Registration is managed by Firebase; Console campaigns need no backend token sync.
}
