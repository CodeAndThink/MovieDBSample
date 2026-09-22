# Detail — AI rules

- DetailViewModel → ApiClients.getMovieDetails; DownloadViewModel riêng → use case. Không gộp hai state.
- `movieId` từ SavedStateHandle phải > 0; sai ID không request/Retry. Chặn request detail trùng.
- Metadata/ảnh thiếu phải có fallback hoặc ẩn; giữ MovieLoadError trong state, map qua NetworkErrorMapper; message dùng presenter/common, không logout Firebase khi TMDB lỗi.
- Tải xuống chỉ mô phỏng WorkManager, không lưu phim; unique work theo movie ID + KEEP.
- UI: xác nhận → PermissionManager → chỉ Start khi allGranted (POST_NOTIFICATIONS từ API 33). Active thì bấm để Cancel.
- Rời màn hình không hủy worker; destroy view phải dismiss dialog/quyền.
- Share: FAB chỉ hiện khi có nội dung; ShareClicked → ShareMovie → Android chooser, dùng URL TMDB theo ID hợp lệ hoặc tên phim khi thiếu ID.
- Test: DetailViewModelTest, DownloadViewModelTest, DownloadInstrumentedTest; quyền, start trùng, cancel, recreate.
