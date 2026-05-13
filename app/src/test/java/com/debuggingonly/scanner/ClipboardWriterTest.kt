package com.debuggingonly.scanner

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardWriterTest {
    @Test
    fun writeCopiesTextToClipboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val writer = ClipboardWriter(context)

        assertTrue(writer.write("https://example.com"))

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("https://example.com", clipboard.primaryClip?.getItemAt(0)?.text.toString())
    }

    @Test
    fun writeRejectsBlankText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val writer = ClipboardWriter(context)

        assertFalse(writer.write("   "))
    }
}
