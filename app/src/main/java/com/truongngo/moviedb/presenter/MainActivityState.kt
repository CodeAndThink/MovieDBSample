package com.truongngo.moviedb.presenter

import com.truongngo.moviedb.presenter.enum.ThemeMode

data class MainActivityState (val isLoading: Boolean = false, val themeMode: ThemeMode = ThemeMode.SYSTEM)