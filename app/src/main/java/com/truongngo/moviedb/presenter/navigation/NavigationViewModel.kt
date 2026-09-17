package com.truongngo.moviedb.presenter.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.truongngo.moviedb.domain.auth.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/** Stores only validated routes and movie IDs, never raw links or credentials, across recreation. */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val links: DeepLinkService,
    private val session: AuthSession
) : ViewModel() {
    val command = savedState.getStateFlow<String?>(COMMAND, null)
    val invalidLink = savedState.getStateFlow(INVALID_LINK, false)

    private var starting = false

    fun start(url: String?, restored: Boolean, onSplash: Boolean = false) {
        if (starting || command.value != null || (restored && !onSplash)) return
        if (!restored) {
            savedState[START_TARGET] = url?.let { links.resolve(it)?.encode() }
            savedState[INVALID_LINK] = url != null && links.resolve(url) == null
        }
        starting = true
        viewModelScope.launch {
            delay(500.milliseconds)
            val target = savedState.get<String>(START_TARGET)?.let(AppDestination::decode)
            savedState[START_TARGET] = null
            starting = false
            navigate(target ?: if (session.isSignedIn()) AppDestination.HOME else AppDestination.LOGIN)
        }
    }

    fun handleLink(url: String): Boolean {
        val destination = links.resolve(url)
        if (destination == null) {
            savedState[INVALID_LINK] = true // Keep the current screen; cold start uses the normal entry.
            return false
        }
        savedState[INVALID_LINK] = false
        if (starting) savedState[START_TARGET] = destination.encode()
        else navigate(destination)
        return true
    }

    fun navigate(destination: AppDestination) {
        val signedIn = session.isSignedIn()
        val target = when {
            destination.requiresAuthentication && !signedIn -> {
                savedState[PENDING] = destination.encode() // Latest protected link wins.
                AppDestination.LOGIN
            }
            !destination.requiresAuthentication && signedIn -> AppDestination.HOME
            else -> destination
        }
        if (target.requiresAuthentication) savedState[PENDING] = null
        savedState[COMMAND] = target.encode()
    }

    fun authenticated() {
        val pending = savedState.get<String>(PENDING)?.let(AppDestination::decode) ?: AppDestination.HOME
        // Recheck the session instead of trusting an incoming URI or UI event.
        navigate(pending)
    }

    fun checkRestoredSession() {
        if (!session.isSignedIn()) navigate(AppDestination.HOME)
    }

    fun back() { savedState[COMMAND] = "BACK" }

    fun login() { savedState[COMMAND] = "BACK_TO_LOGIN" }

    fun logout() {
        session.signOut()
        savedState[PENDING] = null
        savedState[COMMAND] = "RESET_LOGIN"
    }

    fun consumed(value: String) {
        if (command.value == value) savedState[COMMAND] = null
    }

    fun invalidLinkShown() { savedState[INVALID_LINK] = false }

    companion object {
        private const val START_TARGET = "startup_destination"
        private const val COMMAND = "navigation_command"
        private const val PENDING = "pending_destination"
        private const val INVALID_LINK = "invalid_link"
    }
}
