# Implement Login Logic and Connect Components

This plan outlines the integration of `LoginFragment`, `LoginViewModel`, and `AuthRepository` to handle the login flow.

## Proposed Changes

### Domain & Data Layer

#### [MODIFY] [AuthRepositoryImpl.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/data/repository/AuthRepositoryImpl.kt)
- Add `@Inject constructor()` to allow Hilt to provide this implementation.

### Presentation Layer

#### [NEW] [LoginState.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/login/LoginState.kt)
- Define `LoginState` with `isLoading` and `email`/`password` validation states if needed.

#### [NEW] [LoginEffect.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/login/LoginEffect.kt)
- Define `LoginEffect` for one-time events: `LoginSuccess`, `LoginError(message)`, `NavigateToSignup`.

#### [MODIFY] [LoginViewModel.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/login/LoginViewModel.kt)
- Fix `Inject` import (use `javax.inject.Inject`).
- Implement `onLoginClick(email, password)` that calls `authRepository.login`.
- Manage `LoginState` and emit `LoginEffect`.

#### [MODIFY] [LoginFragment.kt](file:///Users/admin/Documents/android_project/Practice/app/src/main/java/com/truongngo/moviedb/presenter/login/LoginFragment.kt)
- Use View Binding (`FragmentLoginBinding`).
- Set up click listener for `btn_login` to call `viewModel.login`.
- Set up click listener for `tv_signup` to navigate.
- Observe `uiState` for loading indicator (via `MainActivityViewModel`).
- Observe `uiEffect` for navigation and error messages.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify compilation and DI setup.

### Manual Verification
1. Open the Login screen.
2. Enter credentials and click Login.
3. Verify that the loading overlay appears.
4. Verify that success/error effects are handled (e.g., Toast or navigation).
