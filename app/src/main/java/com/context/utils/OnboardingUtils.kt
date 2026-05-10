package com.context.utils

import android.content.Context

object OnboardingUtils {
    private const val PREF_NAME = "app_prefs"
    private const val KEY_IS_FIRST_RUN = "is_first_run"
    private const val KEY_USER_NAME = "user_name"

    fun isFirstRun(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_FIRST_RUN, true)
    }

    fun setOnboardingCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_FIRST_RUN, false).commit()
    }

    fun saveUserName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_NAME, name).commit()
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }
}