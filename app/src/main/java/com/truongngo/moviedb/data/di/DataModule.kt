package com.truongngo.moviedb.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.truongngo.moviedb.data.local.datastore.SettingsLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Delegate tạo DataStore khi được truy cập và giữ instance để đọc/ghi preferences trong file "settings".
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Các hàm @Provides hướng dẫn Hilt tạo dependency; @Singleton dùng chung instance trong component.
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // Dùng Context của ứng dụng để lấy DataStore, không giữ Context của Activity/Fragment.
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    // Hilt truyền DataStore đã đăng ký vào lớp phụ trách đọc/ghi cài đặt cục bộ.
    @Provides
    @Singleton
    fun provideSettingsLocalDataSource(dataStore: DataStore<Preferences>): SettingsLocalDataSource {
        return SettingsLocalDataSource(dataStore)
    }
}
