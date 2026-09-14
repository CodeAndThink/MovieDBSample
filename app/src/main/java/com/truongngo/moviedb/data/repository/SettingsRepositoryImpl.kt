package com.truongngo.moviedb.data.repository

import com.truongngo.moviedb.data.local.datastore.SettingsLocalDataSource
import com.truongngo.moviedb.data.mapper.toDomain
import com.truongngo.moviedb.data.mapper.toLocal
import com.truongngo.moviedb.domain.model.SettingsModel
import com.truongngo.moviedb.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(private val dataSource: SettingsLocalDataSource) : SettingsRepository {
    override fun getSettings(): Flow<SettingsModel> {
        return dataSource.getSettings().map { it.toDomain() }
    }

    override suspend fun setSettings(settings: SettingsModel) {
        dataSource.setSettings(settings.toLocal())
    }
}