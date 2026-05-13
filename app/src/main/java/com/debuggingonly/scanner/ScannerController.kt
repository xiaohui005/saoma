package com.debuggingonly.scanner

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onCodeDetected: (String) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private var analyzerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var analyzer: BarcodeAnalyzer? = null
    private var handled = false

    fun start() {
        handled = false
        if (analyzerExecutor.isShutdown) {
            analyzerExecutor = Executors.newSingleThreadExecutor()
        }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val nextAnalyzer = BarcodeAnalyzer { value ->
                    if (!handled) {
                        handled = true
                        onCodeDetected(value)
                    }
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analyzerExecutor, nextAnalyzer) }

                provider.unbindAll()
                closeAnalyzer()
                analyzer = nextAnalyzer
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (error: Exception) {
                onError(error)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                providerFuture.get().unbindAll()
            } catch (error: Exception) {
                onError(error)
            } finally {
                closeAnalyzer()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun resetHandling() {
        handled = false
    }

    fun shutdown() {
        stop()
        analyzerExecutor.shutdown()
    }

    private fun closeAnalyzer() {
        analyzer?.close()
        analyzer = null
    }
}
