package com.debuggingonly.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.Charset

class BarcodeAnalyzerTest {
    @Test
    fun rawValueTakesPriority() {
        val bytes = "错误内容".toByteArray(Charset.forName("GB18030"))

        assertEquals("正确内容", decodeBarcodeValue(" 正确内容 ", bytes))
    }

    @Test
    fun decodesChineseQrBytes() {
        val value = "24-09-16-06-37复试三中三各 5，共5*10"
        val bytes = value.toByteArray(Charset.forName("GB18030"))

        assertEquals(value, decodeBarcodeValue(null, bytes))
    }

    @Test
    fun blankPayloadDoesNothing() {
        assertNull(decodeBarcodeValue("   ", null))
        assertNull(decodeBarcodeValue(null, byteArrayOf()))
    }
}
