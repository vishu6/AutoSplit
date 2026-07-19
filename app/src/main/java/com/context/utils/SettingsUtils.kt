package com.context.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SettingsUtils {
    private const val PREF_NAME = "app_settings"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isHapticsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAPTICS_ENABLED, true)
    }

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit {
            putBoolean(KEY_HAPTICS_ENABLED, enabled)
        }
    }
}
