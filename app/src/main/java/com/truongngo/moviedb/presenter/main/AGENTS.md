# Main — AI rules

- MainFragment chỉ host Home/Settings + bottom navigation; không có ViewModel riêng.
- MainActivity giữ root graph Login/Signup/Main/Search/Detail. Search/Detail ở ngoài bottom navigation.
- Chuyển tab qua NavigationViewModel/AppNavigator; Settings Back về Home.
- Attach child NavController khi tạo view; remove listener + detach khi destroy view.
- Khi sửa MainActivity: chỉ thực thi navigation lúc RESUMED, loading tắt; acknowledge sau thực thi.
- Theme/locale áp ở Activity; giữ restoration không nhân đôi route. Root đã xử lý system-bar insets.
- Test: NavigationInstrumentedTest, NavigationViewModelTest, DeepLinkServiceTest.
