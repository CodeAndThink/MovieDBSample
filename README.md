# MovieDB — Android Practice

## Giới thiệu

MovieDB là ứng dụng Android viết bằng Kotlin, sử dụng dữ liệu từ The Movie Database (TMDB) để khám phá và tra cứu thông tin phim. Dự án phục vụ thực hành xây dựng giao diện Android, quản lý trạng thái, gọi API, xác thực người dùng và xử lý tác vụ nền.

Tên dự án trong Gradle là `Practice`, application ID là `com.truongngo.moviedb`. Giao diện sử dụng XML, Fragment và View Binding.

## Chức năng chính

- Đăng nhập, đăng ký bằng email và mật khẩu qua Firebase Authentication; khôi phục phiên đăng nhập.
- Hiển thị phim đang chiếu, phổ biến, được đánh giá cao và sắp ra mắt.
- Phân trang danh sách phim, kéo để làm mới và thử lại khi gặp lỗi.
- Tìm kiếm phim và xem thông tin chi tiết.
- Chọn giao diện sáng, tối hoặc theo hệ thống; lưu lựa chọn bằng DataStore.
- Chuyển ngôn ngữ Eng/Vi trong Cài đặt; lưu bằng DataStore và áp dụng qua AppCompat. Mặc định là tiếng Anh. Chuỗi giao diện nằm trong `values/strings*.xml` và `values-vi/strings*.xml`; nội dung phim từ TMDB giữ theo dữ liệu API.
- Điều hướng bằng deep link, ví dụ `moviedb://app/detail/550`.
- Mô phỏng tải xuống với tiến độ, thông báo và thao tác hủy bằng WorkManager. Tính năng này không tải hay lưu tệp phim thực tế.

## Kiến trúc

Dự án có một module `app`, chia mã nguồn thành ba lớp theo hướng Clean Architecture, kết hợp MVVM và luồng dữ liệu một chiều ở các màn hình sử dụng `Event`, `State`, `Effect`.

| Lớp | Trách nhiệm |
| --- | --- |
| `presenter` | Fragment hiển thị giao diện; ViewModel xử lý sự kiện và cung cấp trạng thái. Chứa các màn hình và logic điều hướng. |
| `domain` | Định nghĩa model, hợp đồng repository, phiên xác thực và các use case tải xuống. |
| `data` | Triển khai repository, gọi TMDB API, tích hợp Firebase, lưu cài đặt và thực thi tác vụ nền. |

Luồng xử lý điển hình:

```text
Fragment → Event → ViewModel → Repository / Use case → Nguồn dữ liệu
                     ↓
                State / Effect → Fragment cập nhật giao diện hoặc điều hướng
```

- **StateFlow** cung cấp trạng thái giao diện; **Channel/Flow** phát các hiệu ứng một lần ở những màn hình áp dụng mẫu này.
- **Hilt** khởi tạo và cung cấp các dependency như API client, repository và ViewModel.
- Các chức năng xác thực, cài đặt và tải xuống sử dụng hợp đồng repository trong `domain`.
- Hiện tại, các ViewModel của Home, Search và Detail gọi trực tiếp `ApiClients` thuộc lớp `data`; phần dữ liệu phim chưa đi qua repository/use case ở `domain`.

## Cấu trúc thư mục

```text
app/src/main/
├── java/com/truongngo/moviedb/
│   ├── data/
│   │   ├── auth/          # Tích hợp Firebase Authentication
│   │   ├── di/            # Cấu hình dependency injection
│   │   ├── download/      # Worker mô phỏng tải xuống
│   │   ├── local/         # Lưu cài đặt bằng DataStore
│   │   ├── mapper/        # Chuyển đổi model
│   │   ├── network/       # Retrofit, interceptor, API và model phản hồi
│   │   └── repository/    # Triển khai các repository
│   ├── domain/
│   │   ├── auth/          # Hợp đồng phiên xác thực, lỗi đăng ký
│   │   ├── model/         # Model nghiệp vụ
│   │   ├── repository/    # Hợp đồng repository
│   │   └── usecase/       # Các use case tải xuống
│   ├── presenter/         # Home, Search, Detail, Login, Signup, Settings...
│   ├── MainActivity.kt
│   └── PracticeApplication.kt
├── res/                   # Layout XML, navigation, drawable và chuỗi hiển thị
└── AndroidManifest.xml
```

Unit test nằm trong `app/src/test`; kiểm thử trên thiết bị/emulator nằm trong `app/src/androidTest`.

## Công nghệ và thư viện

| Công nghệ / thư viện | Mục đích sử dụng |
| --- | --- |
| Kotlin, Coroutines, Flow | Xử lý bất đồng bộ và quan sát trạng thái |
| AndroidX ViewModel, Lifecycle | Quản lý trạng thái theo vòng đời |
| View Binding, Fragment, Material Components, ConstraintLayout | Xây dựng và liên kết giao diện XML |
| RecyclerView, ViewPager2, SwipeRefreshLayout | Danh sách phim, banner và kéo để làm mới |
| Navigation Component | Điều hướng giữa các màn hình |
| Hilt, KSP | Dependency injection và sinh mã khi build |
| Retrofit, Gson, OkHttp | Gọi API TMDB, chuyển đổi JSON và xử lý HTTP |
| Coil | Tải và hiển thị ảnh phim |
| Firebase Authentication | Đăng nhập, đăng ký và quản lý phiên |
| Firebase Analytics | Dependency phân tích ứng dụng được khai báo qua Firebase BoM |
| Preferences DataStore | Lưu cài đặt giao diện |
| WorkManager | Chạy tác vụ mô phỏng tải xuống ở nền |
| JUnit, Coroutines Test, AndroidX Test, Espresso | Kiểm thử logic và hành vi trên Android |

Phiên bản dependency được khai báo trong `gradle/libs.versions.toml` và `app/build.gradle.kts`.

## Cài đặt và chạy

### 1. Chuẩn bị môi trường

- Android Studio hỗ trợ cấu hình build của dự án: Android Gradle Plugin **9.3.2**, Gradle Wrapper **9.5.0**.
- JDK **21** cho Gradle daemon theo `gradle/gradle-daemon-jvm.properties`. Mức tương thích mã Java được cấu hình là **17**.
- Android SDK **37** để biên dịch (`compileSdk` và `targetSdk` đều là 37).
- Thiết bị hoặc emulator Android **10 / API 29** trở lên.

Mở thư mục dự án bằng Android Studio và cấu hình đường dẫn Android SDK trong `local.properties` nếu IDE chưa tự tạo.

### 2. Cấu hình TMDB

Tạo tệp `.env` ở thư mục gốc và điền thông tin xác thực TMDB của bạn:

```dotenv
MOVIEDB_ACCESS_TOKEN=your_tmdb_read_access_token
MOVIEDB_API_KEY=
```

Chỉ cần cung cấp một trong hai giá trị. Ứng dụng ưu tiên access token; khi token trống sẽ sử dụng API key. Có thể khai báo các biến môi trường cùng tên thay cho `.env`; biến môi trường được ưu tiên khi build.

Tệp `.env` đã được bỏ qua trong `.gitignore`. Không đưa token hoặc API key thực vào README hay mã nguồn. Sau khi thay đổi thông tin xác thực, build lại ứng dụng để cập nhật `BuildConfig`.

### 3. Cấu hình Firebase

- Kiểm tra `app/google-services.json` tương ứng với Firebase project bạn sử dụng và application ID `com.truongngo.moviedb`.
- Trong Firebase Console, bật **Authentication → Sign-in method → Email/Password**.
- Tạo tài khoản thử nghiệm trong Authentication hoặc đăng ký trực tiếp trên ứng dụng.

### 4. Build và khởi chạy

Sync Gradle, chọn module `app` và nhấn **Run** trong Android Studio. Hoặc dùng terminal:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Lệnh `installDebug` yêu cầu thiết bị hoặc emulator đã kết nối. APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`. Trên Windows, dùng `gradlew.bat` thay cho `./gradlew`.

### 5. Build release tối ưu bằng R8

```bash
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

Release bật `optimization.enable = true` (AGP 9.3), bao gồm tối ưu mã, rút gọn tên và loại bỏ tài nguyên không dùng. Quy tắc riêng tại `app/src/main/keepRules/rules.keep` giữ các model và trường JSON dùng qua reflection của Gson. Khi thêm model API, đặt trong package `data.network.model` hoặc bổ sung quy tắc tương ứng.

APK được tạo tại `app/build/outputs/apk/release/app-release-unsigned.apk`; AAB tại `app/build/outputs/bundle/release/app-release.aab`. Cần cấu hình ký release trước khi phát hành. Lưu `app/build/outputs/mapping/release/mapping.txt` cùng từng bản phát hành để khôi phục stack trace đã bị đổi tên. Kiểm tra đăng nhập, danh sách phim, tìm kiếm, chi tiết và tải xuống trên bản release đã ký trước khi phát hành.

## Kiểm thử

Chạy unit test:

```bash
./gradlew :app:testDebugUnitTest
```

Chạy kiểm thử trên thiết bị hoặc emulator đã kết nối:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Các bộ kiểm thử hiện có bao phủ những phần như ViewModel, repository xác thực, xử lý mạng, điều hướng, deep link và mô phỏng tải xuống. Việc đăng nhập Firebase thực tế cần cấu hình dịch vụ và tài khoản hợp lệ.

## Tài liệu chi tiết

- [Hướng dẫn ngắn cho AI](AGENTS.md) — rule chung; rule từng màn hình nằm trong `presenter/<feature>/AGENTS.md`.

- [Trang chủ và danh sách phim](docs/home.md)
- [Tầng mạng và TMDB API](docs/network.md)
- [Đăng nhập và đăng ký](docs/authentication.md)
- [Điều hướng và deep link](docs/navigation.md)
- [Mô phỏng tải xuống](docs/download.md)
