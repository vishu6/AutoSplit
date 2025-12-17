package com.context.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.CategoryEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ExpenseNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val PAYMENT_APPS = setOf(
        "com.google.android.apps.nbu.paisa.user", // GPay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "com.freecharge.android",
        "com.amazon.mShop.android.shopping"       // Amazon Pay often comes via main app
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        
        if (!PAYMENT_APPS.contains(packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        
        Log.d("ContextListener", "Raw Notification: $packageName | $title | $text")

        parseAndSaveExpense(packageName, title, text)
    }

    private fun parseAndSaveExpense(appPackage: String, title: String, message: String) {
        val amountRegex = Pattern.compile("(?i)(?:paid|sent|debited)\\s*(?:₹|Rs\\.?|INR)\\s*([\\d,]+(\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
        val merchantRegex = Pattern.compile("(?i)(?:to|at)\\s+([a-zA-Z0-9 ]+)", Pattern.CASE_INSENSITIVE)

        val amountMatcher = amountRegex.matcher(message)
        val merchantMatcher = merchantRegex.matcher(message)

        if (amountMatcher.find()) {
            val rawAmount = amountMatcher.group(1)?.replace(",", "") ?: "0"
            val amount = rawAmount.toDoubleOrNull() ?: 0.0
            
            var merchant = "Unknown Merchant"
            if (merchantMatcher.find()) {
                merchant = merchantMatcher.group(1)?.trim() ?: "Unknown"
            }

            if (merchant.contains("UPI", ignoreCase = true)) merchant = "UPI Transfer"

            Log.i("ContextListener", "💰 DETECTED: ₹$amount at $merchant")

            serviceScope.launch {
                val predictedCategory = CategoryEngine.predictCategory(merchant)

                val expense = Expense(
                    merchant = merchant,
                    amount = amount,
                    timestamp = System.currentTimeMillis(),
                    category = predictedCategory.label,
                    groupId = null,
                    isAuto = true // MARK AS AUTO
                )
                val db = ExpenseDatabase.getDatabase(applicationContext)
                db.expenseDao().insert(expense)
                
                val intent = Intent("com.context.app.NEW_EXPENSE")
                intent.putExtra("amount", amount)
                intent.putExtra("merchant", merchant)
                sendBroadcast(intent)
            }
        }
    }
}
