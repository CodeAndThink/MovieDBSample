# Settings — AI rules

- SettingsViewModel scope `main_navigation` → SettingsRepository → DataStore; không có Event/Effect riêng.
- Theme SYSTEM/LIGHT/DARK (mặc định SYSTEM); language EN/VI (mặc định EN).
- Ghi riêng từng preference; đổi language không ghi đè theme và ngược lại.
- Giữ isRendering khi cập nhật radio để tránh listener ghi ngược. MainActivity áp theme/locale, có thể recreate.
- Language chỉ đổi UI resources, chưa đổi ngôn ngữ nội dung TMDB.
- Logout qua NavigationViewModel. ThemeMode hiện thuộc presenter nhưng domain/data còn dùng; không mở rộng phụ thuộc này.
- Test: SettingsPersistenceTest và navigation tests; persistence, recreate, logout.
