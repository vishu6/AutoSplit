package com.context.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.UpdateAvailability

object UpdateUtils {
    var showUpdateBanner by mutableStateOf(false)
    var pendingAppUpdateInfo: AppUpdateInfo? = null

    fun checkForUpdateSilently(appUpdateManager: AppUpdateManager) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                showUpdateBanner = true
                pendingAppUpdateInfo = appUpdateInfo
            }
        }
    }
}
