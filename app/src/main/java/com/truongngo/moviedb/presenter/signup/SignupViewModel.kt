package com.truongngo.moviedb.presenter.signup

import androidx.core.util.PatternsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.auth.SignupException
import com.truongngo.moviedb.domain.repository.AuthRepository
import com.truongngo.moviedb.presenter.enum.LoadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val _stateFlow = MutableStateFlow(SignupState())
    val stateFlow = _stateFlow.asStateFlow()
    private val _uiEffect = Channel<SignupEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: SignupEvent) {
        if (_stateFlow.value.loadStatus in setOf(LoadStatus.LOADING, LoadStatus.SUCCESS)) return
        when (event) {
            is SignupEvent.EmailChanged -> _stateFlow.update {
                it.copy(email = event.value, emailError = false, error = null)
            }
            is SignupEvent.PasswordChanged -> _stateFlow.update {
                it.copy(password = event.value, passwordError = false, confirmPasswordError = false, error = null)
            }
            is SignupEvent.ConfirmPasswordChanged -> _stateFlow.update {
                it.copy(confirmPassword = event.value, confirmPasswordError = false, error = null)
            }
            SignupEvent.SignupClicked -> signUp()
            SignupEvent.LoginClicked -> viewModelScope.launch { _uiEffect.send(SignupEffect.NavigateLogin) }
        }
    }

    private fun signUp() {
        val state = _stateFlow.value
        val email = state.email.trim()
        val emailError = !PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()
        // Firebase can enforce a stricter project-specific policy; provider errors handle that case.
        val passwordError = state.password.isBlank() || state.password.length < 6
        val confirmPasswordError = state.confirmPassword.isEmpty() || state.confirmPassword != state.password
        if (emailError || passwordError || confirmPasswordError) {
            _stateFlow.update {
                it.copy(emailError = emailError, passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError, error = null)
            }
            return
        }
        _stateFlow.update { it.copy(loadStatus = LoadStatus.LOADING, error = null) }
        viewModelScope.launch {
            try {
                val user = authRepository.signUp(email, state.password).getOrThrow()
                _stateFlow.update {
                    it.copy(loadStatus = LoadStatus.SUCCESS, user = user, password = "", confirmPassword = "")
                }
                _uiEffect.send(SignupEffect.NavigateHome)
            } catch (exception: CancellationException) {
                _stateFlow.update { it.copy(loadStatus = LoadStatus.INITIAL) }
                throw exception
            } catch (exception: Exception) {
                _stateFlow.update {
                    it.copy(loadStatus = LoadStatus.FAILURE,
                        error = (exception as? SignupException)?.reason ?: SignupException.Reason.UNKNOWN)
                }
            }
        }
    }
}
