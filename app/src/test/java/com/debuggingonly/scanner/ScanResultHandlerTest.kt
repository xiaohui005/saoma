package com.debuggingonly.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultHandlerTest {
    @Test
    fun nullInputDoesNothing() {
        val events = mutableListOf<String>()
        val handler = ScanResultHandler(
            copyText = {
                events += "copy"
                true
            },
            showMessage = { events += it },
            moveToBackground = { events += "background" },
        )

        assertFalse(handler.handle(null))

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun blankInputDoesNothing() {
        val events = mutableListOf<String>()
        val handler = ScanResultHandler(
            copyText = {
                events += "copy"
                true
            },
            showMessage = { events += it },
            moveToBackground = { events += "background" },
        )

        assertFalse(handler.handle("   "))

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun trimsInputBeforeCopying() {
        val copied = mutableListOf<String>()
        val handler = ScanResultHandler(
            copyText = {
                copied += it
                true
            },
            showMessage = {},
            moveToBackground = {},
        )

        assertTrue(handler.handle("  abc123  "))

        assertEquals(listOf("abc123"), copied)
    }

    @Test
    fun successfulCopyRequestsBackground() {
        val events = mutableListOf<String>()
        val handler = ScanResultHandler(
            copyText = { it == "abc123" },
            showMessage = { events += it },
            moveToBackground = { events += "background" },
        )

        assertTrue(handler.handle("abc123"))

        assertEquals(listOf("已复制", "background"), events)
    }

    @Test
    fun failedCopyDoesNotMoveToBackground() {
        val events = mutableListOf<String>()
        val handler = ScanResultHandler(
            copyText = { false },
            showMessage = { events += it },
            moveToBackground = { events += "background" },
        )

        assertFalse(handler.handle("abc123"))

        assertEquals(listOf("复制失败"), events)
    }
}
