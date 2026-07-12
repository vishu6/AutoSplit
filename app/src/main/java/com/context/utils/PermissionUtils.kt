package com.context.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

object PermissionUtils {
    private const val PREF_NAME = "permission_prefs"
    private const val KEY_NUDGE_COUNT = "nudge_count"
    private const val KEY_LAST_NUDGE_TIME = "last_nudge_time"
    private const val KEY_PERMANENT_DISMISS = "perm_dismiss_tracking"
    private const val KEY_BANNER_DISMISSED_AT_COUNT = "banner_dismissed_at_count"
    private const val KEY_BATTERY_BANNER_DISMISSED = "battery_banner_dismissed"
    private const val KEY_WEEKLY_SUMMARY_ENABLED = "weekly_summary_enabled"

    fun isNotificationServiceEnabled(context: Context): Boolean {
        val enabledPackageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackageNames.contains(context.packageName)
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        Log.d("PermissionUtils", "Is battery optimization ignored for ${context.packageName}: $ignored")
        return ignored
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        Log.d("PermissionUtils", "Attempting to request ignore battery optimization")
        try {
            // First try the direct "Allow" dialog (requires REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission)
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("PermissionUtils", "Direct request intent started")
        } catch (e: Exception) {
            Log.e("PermissionUtils", "Direct request failed, trying settings fallback", e)
            try {
                // Fallback to the settings list where user can find our app
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d("PermissionUtils", "Fallback settings intent started")
            } catch (e2: Exception) {
                Log.e("PermissionUtils", "All battery optimization intents failed", e2)
            }
        }
    }

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getNudgeCount(context: Context) = getPrefs(context).getInt(KEY_NUDGE_COUNT, 0)

    fun getLastNudgeTimestamp(context: Context) = getPrefs(context).getLong(KEY_LAST_NUDGE_TIME, 0L)

    fun isPermanentDismissed(context: Context) = getPrefs(context).getBoolean(KEY_PERMANENT_DISMISS, false)

    fun setPermanentDismissed(context: Context) {
        getPrefs(context).edit { putBoolean(KEY_PERMANENT_DISMISS, true) }
    }

    fun recordNudgeDismissed(context: Context) {
        val currentCount = getNudgeCount(context)
        getPrefs(context).edit {
            putInt(KEY_NUDGE_COUNT, currentCount + 1)
            putLong(KEY_LAST_NUDGE_TIME, System.currentTimeMillis())
            // When a new nudge happens, we reset the banner dismissal
            putInt(KEY_BANNER_DISMISSED_AT_COUNT, -1)
        }
    }

    fun setBannerDismissed(context: Context) {
        val currentCount = getNudgeCount(context)
        getPrefs(context).edit { putInt(KEY_BANNER_DISMISSED_AT_COUNT, currentCount) }
    }

    fun isBannerDismissedForCurrentCount(context: Context): Boolean {
        return getPrefs(context).getInt(KEY_BANNER_DISMISSED_AT_COUNT, -1) == getNudgeCount(context)
    }

    fun setBatteryBannerDismissed(context: Context, dismissed: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BATTERY_BANNER_DISMISSED, dismissed) }
    }

    fun isBatteryBannerDismissed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_BANNER_DISMISSED, false)
    }

    fun isWeeklySummaryEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WEEKLY_SUMMARY_ENABLED, true) // Default to true
    }

    fun setWeeklySummaryEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_WEEKLY_SUMMARY_ENABLED, enabled) }
    }
}
