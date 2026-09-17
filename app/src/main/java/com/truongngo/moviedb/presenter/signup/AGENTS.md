# Signup — AI rules

- Luồng: SignupFragment → SignupEvent → SignupViewModel → AuthRepository.signUp.
- Email hợp lệ; password không blank, >= 6 ký tự; confirm trùng hoàn toàn. Firebase có thể áp rule chặt hơn.
- LOADING/SUCCESS chặn event; khi loading khóa form/submit/Back. Giữ lỗi provider trong state qua recreate.
- Thành công xóa password + confirm rồi navigation.authenticated(); quay Login giữ pending route.
- Không lưu password vào Android saved state; không tạo Firestore accounts document.
- Test: SignupViewModelTest, NavigationInstrumentedTest; validation, duplicate submit, retry, clear secrets.
