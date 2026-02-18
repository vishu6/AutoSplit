package com.context.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object PermissionUtils {

    fun isNotificationServiceEnabled(context: Context): Boolean {
        val enabledPackageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackageNames.contains(context.packageName)
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }
}
