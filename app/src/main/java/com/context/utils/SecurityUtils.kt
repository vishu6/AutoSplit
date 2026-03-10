package com.context.utils

import android.content.Context
import android.content.SharedPreferences

object SecurityUtils {
    private const val PREFS_NAME = "security_prefs"
    private const val SECURITY_ENABLED_KEY = "security_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isSecurityEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(SECURITY_ENABLED_KEY, false)
    }

    fun setSecurityEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(SECURITY_ENABLED_KEY, enabled).apply()
    }
}
