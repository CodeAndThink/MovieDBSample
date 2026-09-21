package com.truongngo.moviedb.presenter.login

import com.truongngo.moviedb.domain.auth.RememberedEmailStore
import com.truongngo.moviedb.domain.model.AuthUser
import com.truongngo.moviedb.domain.repository.AuthRepository
import com.truongngo.moviedb.domain.usecase.LoginUseCase
import com.truongngo.moviedb.presenter.enum.LoadStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = MemoryStore()
    private var failLogin = false
    private var passwordReceived = ""
    private val repository = object : AuthRepository {
        override suspend fun login(email: String, password: String): Result<AuthUser> {
            passwordReceived = password
            return if (failLogin) Result.failure(IllegalStateException())
            else Result.success(AuthUser("1", email, null, false))
        }
        override suspend fun signUp(email: String, password: String): Result<AuthUser> = error("unused")
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun restoresEmailAndCheckboxWithoutPassword() = runTest(dispatcher) {
        store.email = "saved@example.com"
        val vm = LoginViewModel(LoginUseCase(repository, store), store)
        runCurrent()
        assertEquals("saved@example.com", vm.stateFlow.value.email)
        assertTrue(vm.stateFlow.value.rememberMe)
        assertEquals("", vm.stateFlow.value.password)
    }
    @Test fun lateReadDoesNotOverwriteUserInput() = runTest(dispatcher) {
        store.email = "saved@example.com"
        store.readGate = CompletableDeferred()
        val vm = LoginViewModel(LoginUseCase(repository, store), store)
        runCurrent()
        vm.onEvent(LoginEvent.EmailChanged("new@example.com"))
        vm.onEvent(LoginEvent.RememberMeChanged(false))
        store.readGate!!.complete(Unit)
        runCurrent()
        assertEquals("new@example.com", vm.stateFlow.value.email)
        assertFalse(vm.stateFlow.value.rememberMe)
        assertNull(store.email)
    }
    @Test fun successfulLoginSavesTrimmedEmailAndPreservesPasswordInput() = runTest(dispatcher) {
        val vm = LoginViewModel(LoginUseCase(repository, store), store)
        runCurrent()
        fill(vm)
        vm.onEvent(LoginEvent.RememberMeChanged(true))
        vm.onEvent(LoginEvent.LoginClicked)
        runCurrent()
        assertEquals("new@example.com", store.email)
        assertEquals(" secret ", passwordReceived)
        assertEquals("", vm.stateFlow.value.password)
        assertEquals(LoginEffect.NavigateHome, vm.uiEffect.first())
    }
    @Test fun failedLoginDoesNotReplaceRememberedEmail() = runTest(dispatcher) {
        store.email = "saved@example.com"
        failLogin = true
        val vm = LoginViewModel(LoginUseCase(repository, store), store)
        runCurrent()
        fill(vm)
        vm.onEvent(LoginEvent.LoginClicked)
        runCurrent()
        assertEquals("saved@example.com", store.email)
        assertEquals(LoadStatus.FAILURE, vm.stateFlow.value.loadStatus)
    }
    @Test fun uncheckingClearsImmediatelyButKeepsTypedEmail() = runTest(dispatcher) {
        store.email = "saved@example.com"
        val vm = LoginViewModel(LoginUseCase(repository, store), store)
        runCurrent()
        vm.onEvent(LoginEvent.RememberMeChanged(false))
        runCurrent()
        assertNull(store.email)
        assertEquals("saved@example.com", vm.stateFlow.value.email)
        fill(vm)
        vm.onEvent(LoginEvent.LoginClicked)
        runCurrent()
        assertNull(store.email)
    }
    @Test fun storageFailureDoesNotFailAuthenticatedLogin() = runTest(dispatcher) {
        store.fail = true
        val vm = LoginViewModel(LoginUseCase(repository, store), store)
        runCurrent()
        assertEquals("", vm.stateFlow.value.email)
        fill(vm)
        vm.onEvent(LoginEvent.RememberMeChanged(true))
        vm.onEvent(LoginEvent.LoginClicked)
        runCurrent()
        assertEquals(LoadStatus.SUCCESS, vm.stateFlow.value.loadStatus)
        assertEquals(LoginEffect.NavigateHome, vm.uiEffect.first())
    }
    @Test fun earlierUncheckCannotEraseEmailSavedByFollowingLogin() = runTest(dispatcher) {
        store.email = "saved@example.com"
        val vm = LoginViewModel(LoginUseCase(repository, store), store)
        runCurrent()
        store.clearGate = CompletableDeferred()
        vm.onEvent(LoginEvent.RememberMeChanged(false))
        runCurrent()
        fill(vm)
        vm.onEvent(LoginEvent.RememberMeChanged(true))
        vm.onEvent(LoginEvent.LoginClicked)
        vm.onEvent(LoginEvent.LoginClicked)
        runCurrent()
        assertEquals(LoadStatus.LOADING, vm.stateFlow.value.loadStatus)
        store.clearGate!!.complete(Unit)
        runCurrent()
        assertEquals("new@example.com", store.email)
        assertEquals(LoadStatus.SUCCESS, vm.stateFlow.value.loadStatus)
        assertEquals("", vm.stateFlow.value.password)
        assertEquals(LoginEffect.NavigateHome, vm.uiEffect.first())
    }

    private fun fill(vm: LoginViewModel) {
        vm.onEvent(LoginEvent.EmailChanged(" new@example.com "))
        vm.onEvent(LoginEvent.PasswordChanged(" secret "))
    }
    private class MemoryStore : RememberedEmailStore {
        var email: String? = null
        var fail = false
        var readGate: CompletableDeferred<Unit>? = null
        var clearGate: CompletableDeferred<Unit>? = null
        override suspend fun read(): String? {
            val snapshot = email
            readGate?.await()
            check(!fail)
            return snapshot
        }
        override suspend fun save(email: String) { check(!fail); this.email = email }
        override suspend fun clear() { clearGate?.await(); check(!fail); email = null }
    }
}
