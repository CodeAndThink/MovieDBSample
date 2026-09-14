package com.truongngo.moviedb

import android.app.NotificationManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.truongngo.moviedb.data.repository.DownloadRepositoryImpl
import com.truongngo.moviedb.domain.model.DownloadStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadInstrumentedTest {
    @Test fun foregroundSimulationReportsProgressDeduplicatesCancelsAndCompletes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val repository = DownloadRepositoryImpl(context)
        val movieId = 900001
        ActivityScenario.launch(MainActivity::class.java).use {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            try {
                repository.start(movieId, "Download simulation test")
                val running = withTimeout(15000) { repository.observe(movieId).first { it.status == DownloadStatus.RUNNING && it.percent > 0 } }
                assertTrue(running.percent in 1..99)
                val notifications = context.getSystemService(NotificationManager::class.java).activeNotifications
                assertTrue(notifications.any { it.id == movieId })
                repository.start(movieId, "Duplicate")
                val work = WorkManager.getInstance(context).getWorkInfosForUniqueWork("movie_download_$movieId").get()
                assertEquals(1, work.count { !it.state.isFinished })
                repository.cancel(movieId)
                withTimeout(5000) { repository.observe(movieId).first { it.status == DownloadStatus.CANCELLED } }
                repository.start(movieId, "Download simulation test")
                val completed = withTimeout(35000) { repository.observe(movieId).first { it.status == DownloadStatus.COMPLETED } }
                assertEquals(100, completed.percent)
            } finally { repository.cancel(movieId) }
        }
    }
}
