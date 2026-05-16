package com.debuggingonly.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var settingsStore: SettingsStore
    private lateinit var clipboardWriter: ClipboardWriter
    private lateinit var scanResultHandler: ScanResultHandler
    private var previewView: PreviewView? = null
    private var scannerController: ScannerController? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showScannerUi()
        } else {
            showPermissionRequiredUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsStore = SettingsStore(this)
        clipboardWriter = ClipboardWriter(this)
        scanResultHandler = ScanResultHandler(
            copyText = { value -> clipboardWriter.write(value) },
            showMessage = { message -> showToast(message) },
            returnToPreviousApp = { finish() },
        )

        updateStandbyService()
        if (hasCameraPermission()) {
            showScannerUi()
        } else {
            requestCameraPermission()
        }
    }

    override fun onDestroy() {
        scannerController?.shutdown()
        scannerController = null
        previewView = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission() && previewView != null) {
            startScanner()
        }
    }

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showPermissionRequiredUi() {
        scannerController?.shutdown()
        scannerController = null
        previewView = null

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        container.addView(TextView(this).apply {
            text = getString(R.string.camera_permission_required)
            gravity = Gravity.CENTER
            textSize = 18f
        })
        container.addView(Button(this).apply {
            text = getString(R.string.retry_permission)
            setOnClickListener { requestCameraPermission() }
        })

        setContentView(container)
    }

    private fun showScannerUi() {
        val currentPreviewView = PreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }
        val standbySwitch = Switch(this).apply {
            text = getString(R.string.standby_enabled)
            isChecked = settingsStore.isStandbyEnabled()
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnCheckedChangeListener { _, checked ->
                settingsStore.setStandbyEnabled(checked)
                if (checked) {
                    requestOverlayPermissionIfNeeded()
                }
                updateStandbyService()
            }
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            addView(standbySwitch)
            addView(currentPreviewView)
        }

        previewView = currentPreviewView
        setContentView(container)
        startScanner()
    }

    private fun startScanner() {
        val currentPreviewView = previewView ?: return
        if (scannerController != null) return

        scannerController = ScannerController(
            context = this,
            lifecycleOwner = this,
            previewView = currentPreviewView,
            onCodeDetected = { value ->
                runOnUiThread {
                    if (!scanResultHandler.handle(value)) {
                        scannerController?.resetHandling()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    showToast(error.localizedMessage ?: error.javaClass.simpleName)
                }
            },
        ).also { it.start() }
    }

    private fun requestOverlayPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) return

        showToast(getString(R.string.overlay_permission_required))
        startActivity(ScannerLaunchIntents.overlayPermissionSettings(this))
    }

    private fun canPostNotifications(): Boolean {
        return android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateStandbyService() {
        if (settingsStore.isStandbyEnabled() && canPostNotifications()) {
            StandbyService.start(this)
        } else {
            StandbyService.stop(this)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
