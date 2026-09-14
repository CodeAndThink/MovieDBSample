package com.truongngo.moviedb.presenter.navigation

import androidx.lifecycle.SavedStateHandle
import com.truongngo.moviedb.domain.auth.AuthSession
import org.junit.Assert.*
import org.junit.Test

class NavigationViewModelTest {
    private class Session(var signedIn: Boolean = false) : AuthSession {
        override fun isSignedIn() = signedIn
        override fun signOut() { signedIn = false }
    }
    private val session = Session()
    private fun model(state: SavedStateHandle = SavedStateHandle()) = NavigationViewModel(
        state, DeepLinkService(), session
    )

    @Test fun protectedLinkContinuesAfterAuthenticationAcrossRecreation() {
        val saved = SavedStateHandle()
        val vm = model(saved)
        vm.start("moviedb://app/settings", false)
        assertEquals("LOGIN", vm.command.value)
        vm.consumed("LOGIN")
        vm.navigate(AppDestination.SIGNUP)
        vm.consumed("SIGNUP")
        val restoredState = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val restored = model(restoredState)
        restored.start("moviedb://app/home", true)
        assertNull(restored.command.value)
        session.signedIn = true
        restored.authenticated()
        assertEquals("SETTINGS", restored.command.value)
        restored.consumed("SETTINGS")
        restored.authenticated()
        assertEquals("HOME", restored.command.value)
    }

    @Test fun latestProtectedLinkWinsAndInvalidLinkDoesNotReplaceIt() {
        val vm = model()
        vm.handleLink("moviedb://app/home")
        vm.handleLink("moviedb://app/settings")
        assertFalse(vm.handleLink("moviedb://app/unknown"))
        assertTrue(vm.invalidLink.value)
        session.signedIn = true
        vm.authenticated()
        assertEquals("SETTINGS", vm.command.value)
    }

    @Test fun authLinksDoNotSignOutExistingSession() {
        session.signedIn = true
        val vm = model()
        vm.handleLink("moviedb://app/signup")
        assertEquals("HOME", vm.command.value)
        assertTrue(session.signedIn)
    }

    @Test fun authenticatedEventCannotBypassSessionCheck() {
        val vm = model()
        vm.authenticated()
        assertEquals("LOGIN", vm.command.value)
    }

    @Test fun invalidColdStartUsesNormalEntryAndRestorationDoesNotReplayIntent() {
        val vm = model()
        vm.start("moviedb://app/unknown", false)
        assertEquals("LOGIN", vm.command.value)
        assertTrue(vm.invalidLink.value)
        vm.consumed("LOGIN")
        vm.start("moviedb://app/signup", true)
        assertNull(vm.command.value)
    }

    @Test fun logoutClearsSessionAndPendingLink() {
        val vm = model()
        vm.handleLink("moviedb://app/settings")
        session.signedIn = true
        vm.logout()
        assertEquals("RESET_LOGIN", vm.command.value)
        assertFalse(session.signedIn)
        session.signedIn = true
        vm.authenticated()
        assertEquals("HOME", vm.command.value)
    }

    @Test fun oldAcknowledgementDoesNotRemoveNewerCommand() {
        val vm = model()
        vm.navigate(AppDestination.LOGIN)
        vm.navigate(AppDestination.SIGNUP)
        vm.consumed("LOGIN")
        assertEquals("SIGNUP", vm.command.value)
    }

    @Test fun detailSurvivesAuthenticationAndSavedStateRestoration() {
        val state = SavedStateHandle()
        val vm = model(state)
        vm.handleLink("moviedb://app/detail/42")
        assertEquals("LOGIN", vm.command.value)
        vm.consumed("LOGIN")
        val restored = model(SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) }))
        session.signedIn = true
        restored.authenticated()
        assertEquals("DETAIL:42", restored.command.value)
    }

    @Test fun latestMovieWinsAndInvalidMovieDoesNotOverwritePendingRoute() {
        val vm = model()
        vm.handleLink("moviedb://app/detail/1")
        vm.handleLink("moviedb://app/detail/2")
        assertFalse(vm.handleLink("moviedb://app/detail/0"))
        session.signedIn = true
        vm.authenticated()
        assertEquals("DETAIL:2", vm.command.value)
    }

    @Test fun movieClickAndDeepLinkProduceSameCommandAndBackIsCentralized() {
        session.signedIn = true
        val vm = model()
        vm.navigate(AppDestination.Detail(42))
        assertEquals("DETAIL:42", vm.command.value)
        vm.consumed("DETAIL:42")
        vm.handleLink("moviedb://app/detail/42")
        assertEquals("DETAIL:42", vm.command.value)
        vm.back()
        assertEquals("BACK", vm.command.value)
    }

    @Test fun signedInStartupOpensHome() {
        session.signedIn = true
        val vm = model()
        vm.start(null, false)
        assertEquals("HOME", vm.command.value)
    }
}
