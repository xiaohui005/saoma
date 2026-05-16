package com.debuggingonly.scanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object ScannerLaunchIntents {
    fun notificationLaunch(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
    }

    fun overlayPermissionSettings(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    }
}
