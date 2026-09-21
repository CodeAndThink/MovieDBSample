# Splash

- Startup-only graph entry; do not expose Splash as a deep link or user navigation route.
- NavigationViewModel waits for both the nonblocking 500 ms delay and permission completion before the session check; preserve pending deep links across recreation.
- Leaving Splash must remove it from the back stack, including signup links.
- Verify with NavigationViewModelTest and :app:assembleDebug.
- Splash tự xin POST_NOTIFICATIONS một lần (API 33+); granted/denied đều tiếp tục. Lưu request đang chờ qua recreation; Settings xử lý retry.
