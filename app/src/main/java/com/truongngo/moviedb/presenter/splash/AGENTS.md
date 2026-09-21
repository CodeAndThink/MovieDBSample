# Splash

- Startup-only graph entry; do not expose Splash as a deep link or user navigation route.
- NavigationViewModel owns the nonblocking 500 ms delay and session check; preserve pending deep links across recreation.
- Leaving Splash must remove it from the back stack, including signup links.
- Verify with NavigationViewModelTest and :app:assembleDebug.
