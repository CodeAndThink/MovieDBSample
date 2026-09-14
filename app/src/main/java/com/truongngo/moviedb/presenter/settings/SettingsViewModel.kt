package com.truongngo.moviedb.presenter.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.model.SettingsModel
import com.truongngo.moviedb.domain.repository.SettingsRepository
import com.truongngo.moviedb.presenter.enum.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<SettingsModel> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsModel(ThemeMode.SYSTEM)
        )

    fun onThemeModeChanged(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setSettings(SettingsModel(themeMode))
        }
    }
}