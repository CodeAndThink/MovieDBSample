@file:Suppress("DEPRECATION")

package com.truongngo.moviedb.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.truongngo.moviedb.domain.auth.RememberedEmailStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedRememberedEmailStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : RememberedEmailStore {
    private val mutex = Mutex()

    // Lazy: Keystore and disk access happen only inside the IO operations below.
    private val preferences by lazy {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, "remembered_email", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        mutex.withLock { preferences.getString("email", null) }
    }

    override suspend fun save(email: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!preferences.edit().putString("email", email).commit()) {
                throw IOException("Could not remember email")
            }
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!preferences.edit().remove("email").commit()) {
                throw IOException("Could not clear remembered email")
            }
        }
    }
}
