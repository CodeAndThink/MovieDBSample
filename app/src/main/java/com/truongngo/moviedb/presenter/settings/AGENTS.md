# Settings — AI rules

- SettingsViewModel scope `main_navigation` → SettingsRepository → DataStore; không có Event/Effect riêng.
- Theme SYSTEM/LIGHT/DARK (mặc định SYSTEM); language EN/VI (mặc định EN).
- Ghi riêng từng preference; đổi language không ghi đè theme và ngược lại.
- Giữ isRendering khi cập nhật radio để tránh listener ghi ngược. MainActivity áp theme/locale, có thể recreate.
- Language chỉ đổi UI resources, chưa đổi ngôn ngữ nội dung TMDB.
- Logout qua NavigationViewModel. ThemeMode hiện thuộc presenter nhưng domain/data còn dùng; không mở rộng phụ thuộc này.
- Test: SettingsPersistenceTest và navigation tests; persistence, recreate, logout.
- Toggle thông báo phản ánh areNotificationsEnabled, refresh ở onResume/callback. Bật dùng PermissionManager nếu thiếu quyền; tắt mở system notification settings. Không lưu quyền vào SettingsRepository; guard khi render, dismiss dialog và tháo listener ở onDestroyView.
- Token FCM: ViewModel → NotificationTokenProvider; StateFlow trong bộ nhớ, chống tải trùng, timeout/retry, truyền CancellationException. Không log/lưu token vào state khôi phục; copy nguyên token, đánh dấu clipboard sensitive. Test SettingsTokenTest.
