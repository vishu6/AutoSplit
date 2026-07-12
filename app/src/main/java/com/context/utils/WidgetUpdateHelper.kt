package com.context.utils

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.context.app.widget.BudgetWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object WidgetUpdateHelper {
    /**
     * Triggers a refresh of all Cleave Budget Widgets.
     * @param wait If true (default), waits for Room to commit transactions. 
     *             Set to false for instant UI-only changes like Privacy Mode.
     */
    fun updateWidget(context: Context, wait: Boolean = true) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (wait) {
                    // Give Room time to commit database changes
                    delay(600)
                }
                
                // Use the most direct Glance update method for speed
                BudgetWidget().updateAll(appContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
