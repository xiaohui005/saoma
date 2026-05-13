package com.debuggingonly.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardWriter(context: Context) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun write(text: String): Boolean {
        val value = text.trim()
        if (value.isEmpty()) return false
        clipboard.setPrimaryClip(ClipData.newPlainText("scanned_content", value))
        return true
    }
}
