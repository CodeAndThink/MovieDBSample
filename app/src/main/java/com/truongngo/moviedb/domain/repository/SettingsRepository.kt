package com.truongngo.moviedb.domain.repository

import com.truongngo.moviedb.domain.model.SettingsModel
import com.truongngo.moviedb.domain.model.AppLanguage
import com.truongngo.moviedb.presenter.enum.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<SettingsModel>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setSettings(settings: SettingsModel)
}