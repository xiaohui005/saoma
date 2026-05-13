package com.debuggingonly.scanner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

class BarcodeAnalyzer(
    private val onCodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
    )

    fun close() {
        scanner.close()
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { codes ->
                codes.firstNotNullOfOrNull { decodeBarcodeValue(it.rawValue, it.rawBytes) }
                    ?.let(onCodeDetected)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

internal fun decodeBarcodeValue(rawValue: String?, rawBytes: ByteArray?): String? {
    rawValue?.trim()?.takeIf(String::isNotEmpty)?.let { return it }
    if (rawBytes == null || rawBytes.isEmpty()) return null

    return listOf("UTF-8", "GB18030", "GBK")
        .firstNotNullOfOrNull { charsetName -> rawBytes.decodeStrict(charsetName) }
        ?.trim()
        ?.takeIf(String::isNotEmpty)
}

private fun ByteArray.decodeStrict(charsetName: String): String? {
    return try {
        Charset.forName(charsetName)
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(this))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }
}
