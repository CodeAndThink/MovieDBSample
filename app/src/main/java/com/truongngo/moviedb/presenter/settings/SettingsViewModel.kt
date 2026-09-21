package com.truongngo.moviedb.presenter.settings

import com.truongngo.moviedb.domain.repository.NotificationTokenProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.model.SettingsModel
import com.truongngo.moviedb.domain.model.AppLanguage
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
    private val settingsRepository: SettingsRepository,
    private val notificationTokenProvider: NotificationTokenProvider
) : ViewModel() {

    val settings: StateFlow<SettingsModel> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsModel(ThemeMode.SYSTEM)
        )

    private val _notificationToken = MutableStateFlow<NotificationTokenState>(NotificationTokenState.Idle)
    val notificationToken = _notificationToken.asStateFlow()

    fun loadNotificationToken() {
        if (_notificationToken.value == NotificationTokenState.Loading) return
        _notificationToken.value = NotificationTokenState.Loading
        viewModelScope.launch {
            try {
                val token = withTimeoutOrNull(15_000) { notificationTokenProvider.getCurrentToken() }
                _notificationToken.value = if (token.isNullOrBlank()) NotificationTokenState.Error
                    else NotificationTokenState.Ready(token)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _notificationToken.value = NotificationTokenState.Error
            } finally {
                if (_notificationToken.value == NotificationTokenState.Loading) {
                    _notificationToken.value = NotificationTokenState.Idle
                }
            }
        }
    }

    fun onLanguageChanged(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun onThemeModeChanged(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }
}
