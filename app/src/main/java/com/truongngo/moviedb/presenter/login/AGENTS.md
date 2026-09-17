# Login — AI rules

- Luồng: LoginFragment → LoginEvent → LoginViewModel → AuthRepository; LoginState/LoginEffect.
- Trim/validate email; password chỉ cần không blank, không áp rule sáu ký tự của Signup.
- LOADING chặn event/form; thành công xóa password rồi gọi navigation.authenticated() qua effect.
- Remember me chỉ là form state; Firebase tự lưu phiên, checkbox chưa điều khiển persistence.
- Loading overlay dùng MainActivityViewModel; onStop phải hideLoading.
- Test liên quan: AuthRepositoryImplTest; chưa có LoginViewModelTest riêng. Kiểm tra validation, lỗi và pending route.
