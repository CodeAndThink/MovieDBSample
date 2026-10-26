package com.truongngo.moviedb.data.di

import com.google.firebase.auth.FirebaseAuth
import com.truongngo.moviedb.data.auth.EmailPasswordAuthProvider
import com.truongngo.moviedb.data.auth.FirebaseEmailPasswordAuthProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Đăng ký dependency xác thực vào component cấp ứng dụng của Hilt.
// @Binds ánh xạ implementation sang contract; @Singleton dùng chung một instance trong component.
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    // Cung cấp kho lưu email đã nhớ bằng implementation lưu trữ mã hóa.
    @Binds
    @Singleton
    abstract fun bindRememberedEmailStore(
        impl: com.truongngo.moviedb.data.local.EncryptedRememberedEmailStore
    ): com.truongngo.moviedb.domain.auth.RememberedEmailStore

    @Binds
    @Singleton
    // Dùng FirebaseAuthSession để đáp ứng contract quản lý phiên đăng nhập.
    abstract fun bindAuthSession(impl: com.truongngo.moviedb.data.auth.FirebaseAuthSession): com.truongngo.moviedb.domain.auth.AuthSession

    @Binds
    @Singleton
    // Chọn Firebase làm implementation xác thực bằng email và mật khẩu.
    abstract fun bindEmailPasswordAuthProvider(
        provider: FirebaseEmailPasswordAuthProvider
    ): EmailPasswordAuthProvider

    companion object {
        @Provides
        @Singleton
        // @Provides chỉ rõ cách lấy dependency; getInstance() lấy FirebaseAuth của Firebase app mặc định.
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
