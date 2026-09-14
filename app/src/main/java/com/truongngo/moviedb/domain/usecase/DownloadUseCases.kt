package com.truongngo.moviedb.domain.usecase

import com.truongngo.moviedb.domain.repository.DownloadRepository
import javax.inject.Inject

class ObserveDownload @Inject constructor(private val repository: DownloadRepository) {
    operator fun invoke(movieId: Int) = repository.observe(movieId)
}
class StartDownload @Inject constructor(private val repository: DownloadRepository) {
    suspend operator fun invoke(movieId: Int, title: String) {
        require(movieId > 0)
        repository.start(movieId, title)
    }
}
class CancelDownload @Inject constructor(private val repository: DownloadRepository) {
    suspend operator fun invoke(movieId: Int) = repository.cancel(movieId)
}
