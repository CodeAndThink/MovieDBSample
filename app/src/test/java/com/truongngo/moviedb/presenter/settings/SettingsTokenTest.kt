package com.truongngo.moviedb.presenter.settings

import com.truongngo.moviedb.domain.repository.NotificationTokenProvider
import com.truongngo.moviedb.domain.repository.SettingsRepository
import com.truongngo.moviedb.domain.model.AppLanguage
import com.truongngo.moviedb.domain.model.SettingsModel
import com.truongngo.moviedb.presenter.enum.ThemeMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsTokenTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private val settings = object : SettingsRepository {
        override fun getSettings() = flowOf(SettingsModel(ThemeMode.SYSTEM))
        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit
        override suspend fun setLanguage(language: AppLanguage) = Unit
        override suspend fun setSettings(settings: SettingsModel) = Unit
    }

    @Test fun duplicateRequestsShareOneLoadAndExposeExactToken() = runTest(dispatcher) {
        val result = CompletableDeferred<String>()
        var calls = 0
        val vm = SettingsViewModel(settings, NotificationTokenProvider { calls++; result.await() })
        vm.loadNotificationToken()
        vm.loadNotificationToken()
        runCurrent()
        assertEquals(1, calls)
        assertEquals(NotificationTokenState.Loading, vm.notificationToken.value)
        result.complete("test-token:abc_123")
        runCurrent()
        assertEquals(NotificationTokenState.Ready("test-token:abc_123"), vm.notificationToken.value)
    }

    @Test fun failureCanRetryAndBlankTokenIsNotCopyable() = runTest(dispatcher) {
        var response = 0
        val vm = SettingsViewModel(settings, NotificationTokenProvider {
            when (response++) { 0 -> throw IllegalStateException(); 1 -> ""; else -> "fresh-token" }
        })
        repeat(2) {
            vm.loadNotificationToken(); runCurrent()
            assertEquals(NotificationTokenState.Error, vm.notificationToken.value)
        }
        vm.loadNotificationToken(); runCurrent()
        assertEquals(NotificationTokenState.Ready("fresh-token"), vm.notificationToken.value)
    }

    @Test fun timeoutEndsLoadingAndCancellationIsNotDisplayedAsError() = runTest(dispatcher) {
        val vm = SettingsViewModel(settings, NotificationTokenProvider { awaitCancellation() })
        vm.loadNotificationToken()
        advanceUntilIdle()
        assertEquals(NotificationTokenState.Error, vm.notificationToken.value)
        val cancelled = SettingsViewModel(settings, NotificationTokenProvider { throw CancellationException() })
        cancelled.loadNotificationToken(); runCurrent()
        assertEquals(NotificationTokenState.Idle, cancelled.notificationToken.value)
    }
}
