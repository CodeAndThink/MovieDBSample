package com.truongngo.moviedb.data.local

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RememberedEmailStoreTest {
    @Test fun emailSurvivesReopenIsEncryptedAndCanBeRemoved() = runBlocking {
        val context = object : ContextWrapper(
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        ) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("test_$name", mode)
        }
        val store = EncryptedRememberedEmailStore(context)
        store.clear()
        try {
            store.save("remember-test@example.com")
            assertEquals("remember-test@example.com", EncryptedRememberedEmailStore(context).read())
            val xml = File(context.applicationInfo.dataDir, "shared_prefs/test_remembered_email.xml").readText()
            assertFalse(xml.contains("remember-test@example.com"))
            store.clear()
            assertNull(EncryptedRememberedEmailStore(context).read())
        } finally { store.clear() }
    }
}
