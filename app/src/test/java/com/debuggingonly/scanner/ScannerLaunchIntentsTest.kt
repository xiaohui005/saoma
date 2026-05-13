package com.debuggingonly.scanner

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScannerLaunchIntentsTest {
    @Test
    fun notificationLaunchStartsTemporaryScannerTask() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = ScannerLaunchIntents.notificationLaunch(context)

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS != 0)
    }
}
