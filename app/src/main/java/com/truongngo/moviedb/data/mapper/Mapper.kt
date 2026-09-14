package com.truongngo.moviedb.data.mapper

import com.truongngo.moviedb.data.local.model.LocalSettings
import com.truongngo.moviedb.domain.model.SettingsModel
import com.truongngo.moviedb.presenter.enum.ThemeMode

fun LocalSettings.toDomain() : SettingsModel {
    return SettingsModel(
        themeMode = ThemeMode.valueOf(themeMode)
    )
}

fun SettingsModel.toLocal() : LocalSettings {
    return LocalSettings(
        themeMode = themeMode.name
    )
}