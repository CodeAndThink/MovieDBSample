package com.truongngo.moviedb.presenter.extension

import com.truongngo.moviedb.presenter.enum.ThemeMode

val ThemeMode.isSystem : Boolean get() = this == ThemeMode.SYSTEM

val ThemeMode.isLight : Boolean get() = this == ThemeMode.LIGHT

val ThemeMode.isDark : Boolean get() = this == ThemeMode.DARK