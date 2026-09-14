package com.truongngo.moviedb.presenter.signup

import com.truongngo.moviedb.domain.auth.SignupException
import com.truongngo.moviedb.domain.model.AuthUser
import com.truongngo.moviedb.domain.repository.AuthRepository
import com.truongngo.moviedb.presenter.enum.LoadStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val user = AuthUser("123", "test@example.com", null, false)
    private var calls = 0
    private var receivedEmail = ""
    private var receivedPassword = ""
    private var response: suspend () -> Result<AuthUser> = { Result.success(user) }
    private val repository = object : AuthRepository {
        override suspend fun login(email: String, password: String): Result<AuthUser> =
            error("Signup must not call login")
        override suspend fun signUp(email: String, password: String): Result<AuthUser> {
            calls++
            receivedEmail = email
            receivedPassword = password
            return response()
        }
    }

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun fill(vm: SignupViewModel, password: String = " secret123 ", confirm: String = password) {
        vm.onEvent(SignupEvent.EmailChanged(" test@example.com "))
        vm.onEvent(SignupEvent.PasswordChanged(password))
        vm.onEvent(SignupEvent.ConfirmPasswordChanged(confirm))
    }

    @Test fun invalidFormDoesNotCallRepository() = runTest(dispatcher) {
        val vm = SignupViewModel(repository)
        vm.onEvent(SignupEvent.EmailChanged("invalid"))
        vm.onEvent(SignupEvent.PasswordChanged("123"))
        vm.onEvent(SignupEvent.ConfirmPasswordChanged("321"))
        vm.onEvent(SignupEvent.SignupClicked)
        runCurrent()
        assertEquals(0, calls)
        assertTrue(vm.stateFlow.value.emailError)
        assertTrue(vm.stateFlow.value.passwordError)
        assertTrue(vm.stateFlow.value.confirmPasswordError)
    }

    @Test fun successTrimsEmailPreservesPasswordAndClearsSecrets() = runTest(dispatcher) {
        val vm = SignupViewModel(repository)
        fill(vm)
        vm.onEvent(SignupEvent.SignupClicked)
        runCurrent()
        assertEquals("test@example.com", receivedEmail)
        assertEquals(" secret123 ", receivedPassword)
        assertEquals(user, vm.stateFlow.value.user)
        assertEquals(LoadStatus.SUCCESS, vm.stateFlow.value.loadStatus)
        assertEquals("", vm.stateFlow.value.password)
        assertEquals("", vm.stateFlow.value.confirmPassword)
        assertEquals(SignupEffect.NavigateHome, vm.uiEffect.first())
        vm.onEvent(SignupEvent.SignupClicked)
        runCurrent()
        assertEquals(1, calls)
    }

    @Test fun pendingRequestBlocksDuplicateSubmitsAndEditing() = runTest(dispatcher) {
        val pending = CompletableDeferred<Result<AuthUser>>()
        response = { pending.await() }
        val vm = SignupViewModel(repository)
        fill(vm)
        vm.onEvent(SignupEvent.SignupClicked)
        vm.onEvent(SignupEvent.SignupClicked)
        vm.onEvent(SignupEvent.EmailChanged("other@example.com"))
        runCurrent()
        assertEquals(1, calls)
        assertEquals(LoadStatus.LOADING, vm.stateFlow.value.loadStatus)
        assertEquals(" test@example.com ", vm.stateFlow.value.email)
        pending.complete(Result.success(user))
        runCurrent()
    }

    @Test fun failureKeepsFormAndAllowsRetry() = runTest(dispatcher) {
        response = { Result.failure(SignupException(SignupException.Reason.EMAIL_IN_USE)) }
        val vm = SignupViewModel(repository)
        fill(vm)
        vm.onEvent(SignupEvent.SignupClicked)
        runCurrent()
        assertEquals(LoadStatus.FAILURE, vm.stateFlow.value.loadStatus)
        assertEquals(SignupException.Reason.EMAIL_IN_USE, vm.stateFlow.value.error)
        assertEquals(" secret123 ", vm.stateFlow.value.password)
        vm.onEvent(SignupEvent.EmailChanged("other@example.com"))
        assertNull(vm.stateFlow.value.error)
        response = { Result.success(user) }
        vm.onEvent(SignupEvent.SignupClicked)
        runCurrent()
        assertEquals(2, calls)
        assertEquals(LoadStatus.SUCCESS, vm.stateFlow.value.loadStatus)
    }

    @Test fun changingPasswordClearsMismatchForRevalidation() = runTest(dispatcher) {
        val vm = SignupViewModel(repository)
        fill(vm, "secret123", "different")
        vm.onEvent(SignupEvent.SignupClicked)
        assertTrue(vm.stateFlow.value.confirmPasswordError)
        vm.onEvent(SignupEvent.PasswordChanged("different"))
        assertFalse(vm.stateFlow.value.confirmPasswordError)
        vm.onEvent(SignupEvent.SignupClicked)
        runCurrent()
        assertEquals(1, calls)
    }

    @Test fun loginLinkEmitsNavigationWithoutRegistering() = runTest(dispatcher) {
        val vm = SignupViewModel(repository)
        vm.onEvent(SignupEvent.LoginClicked)
        runCurrent()
        assertEquals(SignupEffect.NavigateLogin, vm.uiEffect.first())
        assertEquals(0, calls)
    }
}
