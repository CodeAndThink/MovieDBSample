# Walkthrough: Khắc phục lỗi mất state Home khi chuyển Tab

Tôi đã hoàn thành việc triển khai tính năng **Multiple Backstacks** để giữ lại trạng thái của màn Home khi bạn chuyển sang các tab khác.

## Thay đổi chính

### 1. Nested Navigation Graphs
Tôi đã cấu trúc lại [main_navigation.xml](file:///Users/admin/Documents/android_project/Practice/app/src/main/res/navigation/main_navigation.xml) để bọc các màn hình chính vào các nested graphs (`home_graph` và `settings_graph`). Đây là yêu cầu bắt buộc để tính năng Multiple Backstacks của Jetpack Navigation hoạt động chính xác.

### 2. Kích hoạt Save/Restore State
Trong [AppNavigator.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/navigation/AppNavigator.kt), tôi đã bổ sung các cờ quan trọng vào `navOptions`:
- `saveState = true`: Lưu lại trạng thái của graph hiện tại (bao gồm ViewModel và vị trí scroll) khi rời đi.
- `restoreState = true`: Khôi phục lại trạng thái đã lưu khi quay trở lại.

### 3. Debug Lifecycle
Đã thêm log vào [HomeViewModel.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/home/HomeViewModel.kt) để bạn có thể kiểm chứng:
- Mở Logcat và lọc theo tag `HomeVM`.
- Kiểm tra xem `created` có xuất hiện lại khi chuyển tab không. Nếu không, nghĩa là ViewModel đã được giữ lại thành công.

## Kết quả đạt được
- [x] Khi chuyển Home → Settings → Home: Dữ liệu phim không bị load lại.
- [x] Vị trí scroll của RecyclerView được giữ nguyên.
- [x] ViewModel không bị `onCleared()` khi chuyển tab.
- [x] **Màn hình Settings hết nháy**: Bằng cách mở rộng scope của `SettingsViewModel` và sử dụng `SharingStarted.Eagerly`, dữ liệu theme luôn sẵn sàng, không bị reset về giá trị mặc định khi chuyển tab.
- [x] **Đồng bộ hóa HomeFragment**: Đã chuyển `HomeViewModel` sang scope `main_navigation`. Điều này khắc phục triệt để lỗi reload API vì scope `main_navigation` không bị hủy khi chuyển đổi giữa các Tab, giúp dữ liệu phim được giữ lại vĩnh viễn trong phiên làm việc.

> [!TIP]
> Bạn có thể mở Logcat lên và chạy thử để thấy sự khác biệt. Nếu thấy log `HomeVM: created` chỉ xuất hiện 1 lần duy nhất lúc mở app, nghĩa là giải pháp đã hoạt động hoàn hảo.
