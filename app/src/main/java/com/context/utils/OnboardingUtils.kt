package com.context.utils

import android.content.Context
import androidx.core.content.edit

object OnboardingUtils {
    private const val PREF_NAME = "app_prefs"
    private const val KEY_IS_FIRST_RUN = "is_first_run"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_UPI_ID = "user_upi_id"

    fun isFirstRun(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_FIRST_RUN, true)
    }

    fun setOnboardingCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_IS_FIRST_RUN, false) }
    }

    fun saveUserName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_USER_NAME, name) }
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    fun saveUpiId(context: Context, upiId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_UPI_ID, upiId) }
    }

    fun getUpiId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UPI_ID, "") ?: ""
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }
}
