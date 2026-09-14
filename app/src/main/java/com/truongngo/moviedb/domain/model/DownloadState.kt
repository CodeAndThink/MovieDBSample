package com.truongngo.moviedb.domain.model

enum class DownloadStatus { IDLE, QUEUED, RUNNING, COMPLETED, CANCELLED, FAILED }
data class DownloadState(val status: DownloadStatus = DownloadStatus.IDLE, val percent: Int = 0) {
    val isActive get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING
}
