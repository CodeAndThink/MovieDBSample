# Detail — AI rules

- DetailViewModel → ApiClients.getMovieDetails; DownloadViewModel riêng → use case. Không gộp hai state.
- `movieId` từ SavedStateHandle phải > 0; sai ID không request/Retry. Chặn request detail trùng.
- Metadata/ảnh thiếu phải có fallback hoặc ẩn; phân biệt lỗi connection/auth/general.
- Tải xuống chỉ mô phỏng WorkManager, không lưu phim; unique work theo movie ID + KEEP.
- UI: xác nhận → PermissionManager → chỉ Start khi allGranted (POST_NOTIFICATIONS từ API 33). Active thì bấm để Cancel.
- Rời màn hình không hủy worker; destroy view phải dismiss dialog/quyền.
- Test: DetailViewModelTest, DownloadViewModelTest, DownloadInstrumentedTest; quyền, start trùng, cancel, recreate.
