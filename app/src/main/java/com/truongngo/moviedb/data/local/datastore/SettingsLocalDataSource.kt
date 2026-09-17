package com.truongngo.moviedb.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.truongngo.moviedb.data.constant.THEME_MODE_KEY
import com.truongngo.moviedb.data.constant.LANGUAGE_KEY
import com.truongngo.moviedb.data.local.model.LocalSettings
import com.truongngo.moviedb.presenter.enum.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsLocalDataSource(private val dataStore: DataStore<Preferences>) {
    fun getSettings(): Flow<LocalSettings> {
        return dataStore.data.map { preferences ->
            LocalSettings(
                themeMode = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name,
                language = preferences[LANGUAGE_KEY] ?: "en"
            )
        }
    }

    suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { it[THEME_MODE_KEY] = themeMode }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[LANGUAGE_KEY] = language }
    }

    suspend fun setSettings(localSettings: LocalSettings) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = localSettings.themeMode
            preferences[LANGUAGE_KEY] = localSettings.language
        }
    }
}
