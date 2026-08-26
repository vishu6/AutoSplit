package com.example.autosplit

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.CategoryEngine
import com.example.autosplit.ExpenseParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "CleaveListener"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        
        // Use the improved ExpenseParser to handle large numbers and bank noise
        val parsedExpense = ExpenseParser.parse(text)

        if (parsedExpense != null) {
            val db = ExpenseDatabase.getDatabase(applicationContext)
            val expenseDao = db.expenseDao()

            // Resolve the app name for the "via" source tag
            val appName = try {
                val pm = applicationContext.packageManager
                val ai = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                "SMS"
            }

            serviceScope.launch {
                // Duplicate check: Same Amount + Within last 3 mins
                val threeMinutesAgo = System.currentTimeMillis() - (180000)
                val duplicateCount = expenseDao.checkDuplicate(parsedExpense.amount, threeMinutesAgo)

                if (duplicateCount > 0) {
                    Log.d(TAG, "⏭️ Duplicate detected, skipping: ₹${parsedExpense.amount}")
                    return@launch
                }

                val category = CategoryEngine.predictCategory(parsedExpense.merchant)
                val newExpense = Expense(
                    merchant = parsedExpense.merchant,
                    amount = parsedExpense.amount,
                    timestamp = System.currentTimeMillis(),
                    category = category.label,
                    isAuto = true,
                    autoSource = appName
                )
                
                try {
                    expenseDao.insert(newExpense)
                    Log.i(TAG, "✅ Auto-logged: ₹${parsedExpense.amount} from ${parsedExpense.merchant} via $appName")
                    
                    // Notify UI to refresh
                    val intent = Intent("com.context.app.NEW_EXPENSE")
                    sendBroadcast(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to save auto-expense", e)
                }
            }
        }
    }
}
