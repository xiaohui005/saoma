package com.debuggingonly.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("scanner_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun standbyEnabledDefaultsToTrue() {
        val store = SettingsStore(context)
        assertTrue(store.isStandbyEnabled())
    }

    @Test
    fun standbyEnabledCanBeDisabled() {
        val store = SettingsStore(context)
        store.setStandbyEnabled(false)
        assertFalse(store.isStandbyEnabled())
    }
}
