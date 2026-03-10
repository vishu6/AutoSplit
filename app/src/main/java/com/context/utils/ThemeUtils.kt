package com.context.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

object ThemeUtils {
    private const val PREF_NAME = "app_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    fun saveThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun getThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
    }

    fun getThemeModeFlow(context: Context): Flow<Int> = callbackFlow {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_THEME_MODE) {
                trySend(p.getInt(KEY_THEME_MODE, THEME_SYSTEM))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
