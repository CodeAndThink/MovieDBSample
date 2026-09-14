package com.truongngo.moviedb.presenter.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.truongngo.moviedb.domain.auth.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Stores only validated routes and movie IDs, never raw links or credentials, across recreation. */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val links: DeepLinkService,
    private val session: AuthSession
) : ViewModel() {
    val command = savedState.getStateFlow<String?>(COMMAND, null)
    val invalidLink = savedState.getStateFlow(INVALID_LINK, false)

    fun start(url: String?, restored: Boolean) {
        if (restored) return // The original launch Intent must not be replayed after rotation.
        if (url != null && handleLink(url)) return
        navigate(if (session.isSignedIn()) AppDestination.HOME else AppDestination.LOGIN)
    }

    fun handleLink(url: String): Boolean {
        val destination = links.resolve(url)
        if (destination == null) {
            savedState[INVALID_LINK] = true // Keep the current screen; cold start uses the normal entry.
            return false
        }
        savedState[INVALID_LINK] = false
        navigate(destination)
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
        private const val COMMAND = "navigation_command"
        private const val PENDING = "pending_destination"
        private const val INVALID_LINK = "invalid_link"
    }
}
