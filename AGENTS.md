# AI project rules

- Android Kotlin, một module `app`; XML + Fragment + View Binding, Hilt, Coroutines/Flow. Không tự chuyển sang Compose hoặc thêm tầng kiến trúc.
- Source: `app/src/main/java/com/truongngo/moviedb/`; `presenter` = UI/ViewModel, `domain` = model/contract/use case, `data` = implementation.
- Home dùng `LoadHomeMoviesUseCase` → `MovieRepository`; Search/Detail hiện gọi `ApiClients` trực tiếp. Auth/Settings/Download dùng domain contract. Giữ pattern hiện có khi sửa cục bộ.
- Fragment render + nhận thao tác; ViewModel xử lý logic/state. StateFlow cho state, Channel/Flow cho effect một lần. Settings/Main không cần ép thêm Event/Effect.
- Collect theo view lifecycle + `repeatOnLifecycle`; giải phóng binding/listener/dialog ở `onDestroyView`. Không giữ View trong ViewModel.
- Coroutine: truyền tiếp CancellationException; chặn request trùng; giữ cơ chế cancel/generation chống response cũ.
- Điều hướng qua NavigationViewModel → AppNavigator. Route mới sửa đồng bộ AppDestination, graph XML, parser, navigator và test.
- Home/Search/Detail/Settings cần phiên. Auth thành công tiếp tục pending route; logout reset Login. Lỗi TMDB không tự logout Firebase.
- UI strings: `res/values` + `values-vi`; giữ light/dark và insets. Không hardcode chuỗi hiển thị.
- Không log/commit credential hoặc lưu password vào SavedStateHandle/DataStore. Không tự trim password.
- Khi sửa màn hình hoặc layout `fragment_<feature>.xml`, đọc `presenter/<feature>/AGENTS.md` (đường dẫn tương đối từ source ở trên).
- Unit tests: `./gradlew :app:testDebugUnitTest`; build Kotlin/XML/DI: `./gradlew :app:assembleDebug`; test thiết bị khi cần: `./gradlew :app:connectedDebugAndroidTest`.
- Chạy kiểm tra phù hợp phạm vi; sửa Markdown chỉ kiểm tra diff. Không khẳng định test đã chạy nếu chưa chạy.
- Giữ file hướng dẫn ngắn: chỉ rule, invariant, bẫy và test liên quan; cập nhật khi hành vi đổi.

## Workflow

- Đọc rule của màn hình trước khi sửa.
- Task nhỏ, yêu cầu rõ: sửa và kiểm tra trực tiếp.
- Feature lớn: làm rõ thiết kế → plan → triển khai → review.
- Bug: tìm nguyên nhân → test tái hiện phù hợp → sửa → kiểm chứng.
- Giữ plan/report ngắn; không tạo tài liệu chỉ để đủ quy trình.
