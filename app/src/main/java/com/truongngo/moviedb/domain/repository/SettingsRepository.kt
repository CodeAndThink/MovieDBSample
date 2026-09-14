package com.truongngo.moviedb.domain.repository

import com.truongngo.moviedb.domain.model.SettingsModel
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<SettingsModel>

    suspend fun setSettings(settings: SettingsModel)
}