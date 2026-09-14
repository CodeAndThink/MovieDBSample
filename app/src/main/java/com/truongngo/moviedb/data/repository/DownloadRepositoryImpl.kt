package com.truongngo.moviedb.data.repository

import android.content.Context
import androidx.work.*
import com.truongngo.moviedb.data.download.SimulatedDownloadWorker
import com.truongngo.moviedb.domain.model.DownloadState
import com.truongngo.moviedb.domain.model.DownloadStatus
import com.truongngo.moviedb.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(@ApplicationContext context: Context) : DownloadRepository {
    private val manager = WorkManager.getInstance(context)
    private fun name(movieId: Int) = "movie_download_$movieId"
    override fun observe(movieId: Int) = manager.getWorkInfosForUniqueWorkFlow(name(movieId)).map { infos ->
        val info = infos.firstOrNull { !it.state.isFinished } ?: infos.firstOrNull()
        if (info == null) DownloadState() else DownloadState(
            status = when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadStatus.QUEUED
                WorkInfo.State.RUNNING -> DownloadStatus.RUNNING
                WorkInfo.State.SUCCEEDED -> DownloadStatus.COMPLETED
                WorkInfo.State.CANCELLED -> DownloadStatus.CANCELLED
                WorkInfo.State.FAILED -> DownloadStatus.FAILED
            },
            percent = if (info.state == WorkInfo.State.SUCCEEDED) 100 else info.progress.getInt(SimulatedDownloadWorker.PROGRESS, 0),
        )
    }

    override suspend fun start(movieId: Int, title: String) {
        val request = OneTimeWorkRequestBuilder<SimulatedDownloadWorker>()
            .setInputData(workDataOf(SimulatedDownloadWorker.MOVIE_ID to movieId, SimulatedDownloadWorker.TITLE to title))
            .build()
        manager.enqueueUniqueWork(name(movieId), ExistingWorkPolicy.KEEP, request).await()
    }
    override suspend fun cancel(movieId: Int) { manager.cancelUniqueWork(name(movieId)).await() }
}
