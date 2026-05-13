package com.debuggingonly.scanner

import android.content.Context

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isStandbyEnabled(): Boolean = preferences.getBoolean(KEY_STANDBY_ENABLED, true)

    fun setStandbyEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_STANDBY_ENABLED, enabled).apply()
    }

    private companion object {
        const val PREFS_NAME = "scanner_settings"
        const val KEY_STANDBY_ENABLED = "standby_enabled"
    }
}
