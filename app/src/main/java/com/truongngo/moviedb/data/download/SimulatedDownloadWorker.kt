package com.truongngo.moviedb.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.truongngo.moviedb.MainActivity
import com.truongngo.moviedb.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Demo only: advances a counter, without network requests or filesystem output. */
class SimulatedDownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (inputData.getInt(MOVIE_ID, 0) <= 0) return Result.failure()
        return try {
            setForeground(foreground(0))
            for (percent in 1..100) {
                delay(200.milliseconds, )
                setProgress(workDataOf(PROGRESS to percent))
                setForeground(foreground(percent))
            }
            Result.success(workDataOf(PROGRESS to 100))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun foreground(percent: Int): ForegroundInfo {
        val context = applicationContext
        val movieId = inputData.getInt(MOVIE_ID, 0)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.download_channel), NotificationManager.IMPORTANCE_LOW))
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("moviedb://app/detail/$movieId")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(inputData.getString(TITLE))
            .setContentText(context.getString(R.string.download_progress, percent))
            .setProgress(100, percent, false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(context, movieId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.download_cancel), WorkManager.getInstance(context).createCancelPendingIntent(id))
            .build()
        return ForegroundInfo(movieId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val MOVIE_ID = "movie_id"
        const val TITLE = "title"
        const val PROGRESS = "progress"
        private const val CHANNEL = "simulated_downloads"
    }
}
