package com.truongngo.moviedb.domain.model

import com.truongngo.moviedb.presenter.enum.ThemeMode

data class SettingsModel(
    val themeMode: ThemeMode,
    val language: AppLanguage = AppLanguage.ENGLISH
)
