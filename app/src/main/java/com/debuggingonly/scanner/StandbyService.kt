package com.debuggingonly.scanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class StandbyService : Service() {
    private var floatingButton: View? = null
    private var windowManager: WindowManager? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        showFloatingButtonIfAllowed()
        return START_STICKY
    }

    override fun onDestroy() {
        removeFloatingButton()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "扫码器待命",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.standby_notification_title))
        .setContentText(getString(R.string.standby_notification_text))
        .setOngoing(true)
        .setContentIntent(openScannerIntent())
        .build()

    private fun openScannerIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            ScannerLaunchIntents.notificationLaunch(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showFloatingButtonIfAllowed() {
        if (floatingButton != null) return
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) return

        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val button = Button(this).apply {
            text = getString(R.string.floating_button_text)
            setOnClickListener {
                startActivity(ScannerLaunchIntents.notificationLaunch(this@StandbyService))
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }

        manager.addView(button, params)
        windowManager = manager
        floatingButton = button
    }

    private fun removeFloatingButton() {
        val button = floatingButton ?: return
        windowManager?.removeView(button)
        floatingButton = null
        windowManager = null
    }

    companion object {
        private const val CHANNEL_ID = "scanner_standby"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, StandbyService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StandbyService::class.java))
        }
    }
}
