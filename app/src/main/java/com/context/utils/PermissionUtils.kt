package com.context.utils

import android.content.Context
import android.provider.Settings

fun isNotificationPermissionGranted(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}
