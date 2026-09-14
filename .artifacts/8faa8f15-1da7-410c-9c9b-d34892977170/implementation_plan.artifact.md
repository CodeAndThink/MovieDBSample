# Kế hoạch khắc phục lỗi mất state khi chuyển Tab (Multiple Backstacks)

Cách tiếp cận này rất ổn và chuyên nghiệp. Nó giúp xác định chính xác vấn đề nằm ở Navigation hay ở ViewModel lifecycle trước khi thực hiện những thay đổi lớn về scope.

## User Review Required

> [!IMPORTANT]
> Tôi sẽ thay đổi cấu trúc file `main_navigation.xml` để hỗ trợ Nested Graphs. Điều này là cần thiết để kích hoạt tính năng Multiple Backstacks một cách chuẩn nhất của Jetpack Navigation.

> [!NOTE]
> Tôi sẽ ưu tiên dùng `setupWithNavController` trong `MainFragment` nếu có thể để tận dụng tối đa khả năng tự động của thư viện, nhưng vẫn giữ quyền điều khiển qua `AppNavigator` cho các logic đặc biệt (như Deep Link).

## Proposed Changes

### [Component] Navigation

#### [MODIFY] [main_navigation.xml](file:///Users/admin/Documents/android_project/Practice/app/src/main/res/navigation/main_navigation.xml)
- Bọc `home` fragment vào `<navigation android:id="@+id/home_graph">`.
- Bọc `settings` fragment vào `<navigation android:id="@+id/settings_graph">`.
- Cập nhật `startDestination` của `main_navigation` trỏ vào `home_graph`.

#### [MODIFY] [AppNavigator.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/navigation/AppNavigator.kt)
- Cập nhật logic `navigate` cho `HOME` và `SETTINGS` để sử dụng các cờ:
    - `launchSingleTop = true`
    - `restoreState = true`
    - `saveState = true` (khi popUpTo)

#### [MODIFY] [MainFragment.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/main/MainFragment.kt)
- Thử nghiệm kết nối trực tiếp `BottomNavigationView` với `NavController` bằng `setupWithNavController`.

### [Component] Home Feature (Debugging)

#### [MODIFY] [HomeViewModel.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/home/HomeViewModel.kt)
- Thêm `Log.d` vào `init` và `onCleared` để theo dõi vòng đời.

## Verification Plan

### Manual Verification
1. **Kiểm tra Lifecycle**: Mở Logcat, filter `HomeVM`. Xem log `created` có xuất hiện lại khi chuyển từ Settings về Home không.
2. **Kiểm tra UI State**: Cuộn danh sách phim ở Home, chuyển sang Settings, quay lại Home. Vị trí cuộn phải được giữ nguyên.
3. **Kiểm tra API**: Kiểm tra Network Profiler hoặc Logcat để đảm bảo API không bị gọi lại vô lý.

### Automated Tests
- Chạy `NavigationInstrumentedTest.kt` (nếu có) để đảm bảo không làm gãy luồng điều hướng hiện tại.
