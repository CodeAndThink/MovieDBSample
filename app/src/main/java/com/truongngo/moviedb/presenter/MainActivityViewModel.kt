package com.truongngo.moviedb.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainActivityState())
    
    val uiState: StateFlow<MainActivityState> = combine(
        _uiState,
        settingsRepository.getSettings()
    ) { state, settings ->
        state.copy(themeMode = settings.themeMode, language = settings.language)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainActivityState()
    )

    fun showLoading() {
        _uiState.update {
            it.copy(
                isLoading = true
            )
        }
    }

    fun hideLoading() {
        _uiState.update {
            it.copy(
                isLoading = false
            )
        }
    }

}