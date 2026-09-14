package com.truongngo.moviedb.presenter.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.repository.AuthRepository
import com.truongngo.moviedb.presenter.enum.LoadStatus
import com.truongngo.moviedb.presenter.extension.isLoading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val _stateFlow = MutableStateFlow(LoginState())
    val stateFlow = _stateFlow.asStateFlow()

    private val _uiEffect = Channel<LoginEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        if (_stateFlow.value.loadStatus.isLoading) return
        when (event) {
            is LoginEvent.EmailChanged -> _stateFlow.update {
                it.copy(email = event.email, emailError = false)
            }
            is LoginEvent.PasswordChanged -> _stateFlow.update {
                it.copy(password = event.password, passwordError = false)
            }
            is LoginEvent.RememberMeChanged -> _stateFlow.update {
                it.copy(rememberMe = event.checked)
            }
            LoginEvent.LoginClicked -> login()
            LoginEvent.SignupClicked -> viewModelScope.launch {
                _uiEffect.send(LoginEffect.NavigateSignup)
            }
        }
    }

    private fun login() {
        val state = _stateFlow.value
        val email = state.email.trim()
        val emailError = !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val passwordError = state.password.isBlank()
        if (emailError || passwordError) {
            _stateFlow.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _stateFlow.update { it.copy(loadStatus = LoadStatus.LOADING) }
        viewModelScope.launch {
            try {
                val user = authRepository.login(email, state.password).getOrThrow()
                _stateFlow.update { it.copy(loadStatus = LoadStatus.SUCCESS, password = "", user = user) }
                _uiEffect.send(LoginEffect.NavigateHome)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _stateFlow.update { it.copy(loadStatus = LoadStatus.FAILURE) }
                _uiEffect.send(LoginEffect.ShowLoginError)
            }
        }
    }
}
