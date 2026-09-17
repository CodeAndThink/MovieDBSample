package com.truongngo.moviedb.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.truongngo.moviedb.data.constant.LANGUAGE_KEY
import com.truongngo.moviedb.data.constant.THEME_MODE_KEY
import com.truongngo.moviedb.data.local.datastore.SettingsLocalDataSource
import com.truongngo.moviedb.data.repository.SettingsRepositoryImpl
import com.truongngo.moviedb.domain.model.AppLanguage
import com.truongngo.moviedb.presenter.enum.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsPersistenceTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun existingThemeGetsEnglishAndUnknownLanguageFallsBack() = runTest {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) {
            directory.root.resolve("legacy.preferences_pb")
        }
        try {
            store.edit { it[THEME_MODE_KEY] = ThemeMode.DARK.name }
            val repository = SettingsRepositoryImpl(SettingsLocalDataSource(store))
            assertEquals(AppLanguage.ENGLISH, repository.getSettings().first().language)
            assertEquals(ThemeMode.DARK, repository.getSettings().first().themeMode)
            store.edit { it[LANGUAGE_KEY] = "unsupported" }
            assertEquals(AppLanguage.ENGLISH, repository.getSettings().first().language)
        } finally { job.cancelAndJoin() }
    }

    @Test fun concurrentChangesPreserveBothPreferencesAfterReopening() = runTest {
        val file = directory.root.resolve("settings.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        try {
            val repository = SettingsRepositoryImpl(SettingsLocalDataSource(store))
            val language = launch { repository.setLanguage(AppLanguage.VIETNAMESE) }
            val theme = launch { repository.setThemeMode(ThemeMode.DARK) }
            language.join()
            theme.join()
        } finally { job.cancelAndJoin() }

        val reopenedJob = SupervisorJob()
        val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + reopenedJob)) { file }
        try {
            val repository = SettingsRepositoryImpl(SettingsLocalDataSource(reopened))
            assertEquals(AppLanguage.VIETNAMESE, repository.getSettings().first().language)
            assertEquals(ThemeMode.DARK, repository.getSettings().first().themeMode)
            repository.setLanguage(AppLanguage.ENGLISH)
            assertEquals(AppLanguage.ENGLISH, repository.getSettings().first().language)
            assertEquals(ThemeMode.DARK, repository.getSettings().first().themeMode)
        } finally { reopenedJob.cancelAndJoin() }
    }
}
