package com.context.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object SecurityUtils {
    private const val PREF_NAME = "security_prefs"
    private const val KEY_SECURITY_ENABLED = "security_enabled"
    private const val KEY_PRIVACY_MODE = "privacy_mode_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isSecurityEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SECURITY_ENABLED, false)
    }

    fun setSecurityEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SECURITY_ENABLED, enabled) }
    }

    fun isPrivacyModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PRIVACY_MODE, false)
    }

    /**
     * Updates Privacy Mode. Uses synchronous commit for immediate Widget sync.
     */
    fun setPrivacyModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit(commit = true) { 
            putBoolean(KEY_PRIVACY_MODE, enabled) 
        }
    }

    fun getPrivacyModeFlow(context: Context): Flow<Boolean> = callbackFlow {
        val prefs = getPrefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_PRIVACY_MODE) {
                trySend(p.getBoolean(KEY_PRIVACY_MODE, false))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_PRIVACY_MODE, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
