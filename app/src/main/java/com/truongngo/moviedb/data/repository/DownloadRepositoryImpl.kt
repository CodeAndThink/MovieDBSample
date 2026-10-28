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
    // Lấy WorkManager dùng chung của ứng dụng để theo dõi, lên lịch và hủy tác vụ tải.
    private val manager = WorkManager.getInstance(context)

    // Tạo tên công việc riêng cho từng phim; dùng cùng tên khi quan sát, bắt đầu hoặc hủy.
    private fun name(movieId: Int) = "movie_download_$movieId"

    // getWorkInfosForUniqueWorkFlow phát danh sách WorkInfo mỗi khi công việc có thay đổi.
    // map chuyển dữ liệu của WorkManager sang DownloadState mà tầng domain/UI sử dụng.
    override fun observe(movieId: Int) = manager.getWorkInfosForUniqueWorkFlow(name(movieId)).map { infos ->
        // Ưu tiên công việc chưa kết thúc; nếu không có thì lấy phần tử đầu tiên còn lưu.
        val info = infos.firstOrNull { !it.state.isFinished } ?: infos.firstOrNull()
        // Chưa có công việc thì trả state mặc định; ngược lại ánh xạ trạng thái tương ứng.
        if (info == null) DownloadState() else DownloadState(
            status = when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadStatus.QUEUED
                WorkInfo.State.RUNNING -> DownloadStatus.RUNNING
                WorkInfo.State.SUCCEEDED -> DownloadStatus.COMPLETED
                WorkInfo.State.CANCELLED -> DownloadStatus.CANCELLED
                WorkInfo.State.FAILED -> DownloadStatus.FAILED
            },
            // getInt đọc tiến độ do worker báo, mặc định 0 nếu chưa có; thành công luôn là 100%.
            percent = if (info.state == WorkInfo.State.SUCCEEDED) 100 else info.progress.getInt(SimulatedDownloadWorker.PROGRESS, 0),
        )
    }

    override suspend fun start(movieId: Int, title: String) {
        // Tạo yêu cầu chạy SimulatedDownloadWorker một lần, không phải tác vụ định kỳ.
        val request = OneTimeWorkRequestBuilder<SimulatedDownloadWorker>()
            // workDataOf đóng gói ID và tiêu đề để worker đọc từ inputData.
            .setInputData(workDataOf(SimulatedDownloadWorker.MOVIE_ID to movieId, SimulatedDownloadWorker.TITLE to title))
            // Hoàn tất cấu hình và tạo WorkRequest để đưa vào hàng đợi.
            .build()
        // KEEP giữ công việc cùng tên nếu nó chưa kết thúc, tránh tạo lượt tải trùng.
        // await() chờ thao tác enqueue hoàn tất, không chờ worker tải xong.
        manager.enqueueUniqueWork(name(movieId), ExistingWorkPolicy.KEEP, request).await()
    }

    // Hủy công việc theo tên của phim; await() chờ thao tác hủy của WorkManager hoàn tất.
    override suspend fun cancel(movieId: Int) { manager.cancelUniqueWork(name(movieId)).await() }
}
