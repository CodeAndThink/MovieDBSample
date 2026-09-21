package com.truongngo.moviedb.presenter.login

import androidx.core.util.PatternsCompat
import com.truongngo.moviedb.domain.auth.RememberedEmailStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truongngo.moviedb.domain.usecase.LoginUseCase
import com.truongngo.moviedb.presenter.enum.LoadStatus
import com.truongngo.moviedb.presenter.extension.isLoading
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val rememberedEmail: RememberedEmailStore
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(LoginState())
    val stateFlow = _stateFlow.asStateFlow()

    private val _uiEffect = Channel<LoginEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private val storageMutex = Mutex()
    private var emailEdited = false
    private var rememberEdited = false

    init {
        viewModelScope.launch {
            val savedEmail = storageOperation { rememberedEmail.read() }
            if (!savedEmail.isNullOrBlank() && !emailEdited && !rememberEdited &&
                _stateFlow.value.loadStatus == LoadStatus.INITIAL
            ) {
                _stateFlow.update { it.copy(email = savedEmail, rememberMe = true) }
            }
        }
    }

    // Serializes restore/clear/save. Storage failure must not undo successful authentication.
    private suspend fun <T> storageOperation(block: suspend () -> T): T? =
        storageMutex.withLock {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }
        }

    fun onEvent(event: LoginEvent) {
        if (_stateFlow.value.loadStatus.isLoading) return
        when (event) {
            is LoginEvent.EmailChanged -> {
                emailEdited = true
                _stateFlow.update { it.copy(email = event.email, emailError = false) }
            }
            is LoginEvent.PasswordChanged -> _stateFlow.update {
                it.copy(password = event.password, passwordError = false)
            }
            is LoginEvent.RememberMeChanged -> {
                rememberEdited = true
                _stateFlow.update { it.copy(rememberMe = event.checked) }
                if (!event.checked) viewModelScope.launch {
                    storageOperation { rememberedEmail.clear() }
                }
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
        val emailError = !PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()
        val passwordError = state.password.isBlank()
        if (emailError || passwordError) {
            _stateFlow.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _stateFlow.update { it.copy(loadStatus = LoadStatus.LOADING) }
        viewModelScope.launch {
            try {
                // Share the lock with restore/uncheck so an earlier clear cannot erase the new saved email.
                val user = storageMutex.withLock {
                    loginUseCase(email, state.password, state.rememberMe)
                }
                _stateFlow.update { it.copy(password = "", loadStatus = LoadStatus.SUCCESS, user = user) }
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
