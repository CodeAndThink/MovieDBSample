package com.truongngo.moviedb.presenter.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.truongngo.moviedb.MainActivity
import com.truongngo.moviedb.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationHandler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: PushNotificationParser,
) {
    fun createChannel() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.push_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.push_channel_description) }
        )
    }

    fun show(payload: PushNotificationPayload) {
        createChannel()
        val manager = NotificationManagerCompat.from(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ||
            !manager.areNotificationsEnabled()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(parser.targetLink(payload.deepLink))
            // Intent identity includes categories, so two messages for the same film stay independent.
            addCategory("${context.packageName}.push.${payload.id}")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name))
            .setContentText(payload.body?.takeIf { it.isNotBlank() } ?: context.getString(R.string.push_default_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.push_default_body)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(payload.id, 0, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and notify.
        }
    }

    companion object { const val CHANNEL_ID = "movie_updates" }
}
