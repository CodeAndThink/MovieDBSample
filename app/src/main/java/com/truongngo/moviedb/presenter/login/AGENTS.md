# Login — AI rules

- Luồng: LoginFragment → LoginEvent → LoginViewModel → AuthRepository; LoginState/LoginEffect.
- Trim/validate email; password chỉ cần không blank, không áp rule sáu ký tự của Signup.
- LOADING chặn event/form; thành công xóa password rồi gọi navigation.authenticated() qua effect.
- Remember email: EncryptedSharedPreferences chỉ lưu email đã trim sau login thành công; bỏ tick xóa ngay, logout giữ email. Không lưu password hoặc thay đổi phiên Firebase.
- Restore không ghi đè thao tác nhập/tick; serialize đọc/xóa/lưu ngoài main thread. Lỗi storage không làm login thất bại; truyền tiếp CancellationException. File remembered_email.xml phải được loại khỏi backup/transfer.
- Loading overlay dùng MainActivityViewModel; onStop phải hideLoading.
- Test liên quan: AuthRepositoryImplTest; LoginViewModelTest và RememberedEmailStoreTest (thiết bị). Kiểm tra validation, lỗi và pending route.
