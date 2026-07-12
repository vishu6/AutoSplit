package com.context.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.context.app.widget.BudgetWidget
import com.context.service.DailySummaryWorker
import com.context.sync.GroupSyncManager
import com.context.ui.theme.ContextTheme
import com.context.utils.ThemeUtils
import com.context.utils.UpdateUtils
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject 
    lateinit var groupSyncManager: GroupSyncManager
    
    private lateinit var appUpdateManager: AppUpdateManager

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)
        UpdateUtils.checkForUpdateSilently(appUpdateManager)

        // Handle Deep Link or Widget Action
        handleIntent(intent)

        setContent {
            val context = LocalContext.current
            val themeMode by ThemeUtils.getThemeModeFlow(context).collectAsState(initial = ThemeUtils.THEME_SYSTEM)
            
            val isDark = when(themeMode) {
                ThemeUtils.THEME_LIGHT -> false
                ThemeUtils.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            ContextTheme(darkTheme = isDark) {
                AppNavigation(appUpdateManager = appUpdateManager)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // 1. Handle Widget Actions (Production stable approach)
        val widgetAction = intent?.getStringExtra(BudgetWidget.KEY_WIDGET_ACTION.name)
        if (widgetAction == BudgetWidget.ACTION_ADD_EXPENSE) {
            // Signal navigation to open Add Expense
            // This can be handled via a deep link or internal navigation event
            val addIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("cleave://add"))
            intent.data = addIntent.data
        }

        // 2. Handle Group Joining and other Deep Links
        intent?.data?.let { uri ->
            if (uri.host == "join" || uri.path?.contains("join") == true) {
                groupSyncManager.joinByUrl(
                    url = uri.toString(),
                    onComplete = { /* AppNavigation handles navigation */ },
                    onError = { /* Log or ignore */ }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(DailySummaryWorker.NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}
