package com.debuggingonly.scanner

class ScanResultHandler(
    private val copyText: (String) -> Boolean,
    private val showMessage: (String) -> Unit,
    private val moveToBackground: () -> Unit,
) {
    fun handle(rawValue: String?): Boolean {
        val value = rawValue?.trim().orEmpty()
        if (value.isEmpty()) return false

        return if (copyText(value)) {
            showMessage("已复制")
            moveToBackground()
            true
        } else {
            showMessage("复制失败")
            false
        }
    }
}
