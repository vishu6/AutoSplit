package com.context.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.context.ui.theme.ContextTheme
import com.context.utils.ThemeUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val themeMode by ThemeUtils.getThemeModeFlow(context).collectAsState(initial = ThemeUtils.THEME_SYSTEM)
            
            val isDark = when(themeMode) {
                ThemeUtils.THEME_LIGHT -> false
                ThemeUtils.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            ContextTheme(darkTheme = isDark) {
                AppNavigation()
            }
        }
    }
}
