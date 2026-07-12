package com.context.service

import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.BudgetUtils
import com.context.utils.CategoryEngine
import com.context.utils.HapticUtils
import com.context.utils.WidgetUpdateHelper
import com.example.autosplit.ExpenseParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "ExpenseListener"

    // EXPANDED package list to cover most Indian Banks and SMS apps
    private val allowedApps = setOf(
        // UPI & Payment Apps
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "in.org.npci.upiapp",
        "com.whatsapp",
        "com.amazon.mShop.android.shopping",
        "in.amazon.mShop.android.shopping",
        "com.mobikwik_new",
        "com.freecharge.android",
        "com.dreamplug.androidapp",
        "com.myairtelapp",
        "com.jio.myjio",
        
        // Major Bank Apps
        "com.csam.icici.bank.imobile",
        "com.snapwork.hdfc",
        "com.sbi.SBIFreedomPlus",
        "com.axis.mobile",
        "com.kotak.mahindra.kotak",
        "com.idbi.mobilebanking",
        "com.indusind.ibmobile",
        "com.yesbank",
        "com.bpb.mobilebanking",
        
        // SMS & Messaging Apps (System & 3rd Party)
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.miui.sms",
        "com.oneplus.mms",
        "com.oppo.mms",
        "com.vivo.mms",
        "com.realme.mms",
        "com.asus.message",
        "com.motorola.messaging",
        "com.truecaller"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        Log.d(TAG, "📩 Received notification from: $packageName")

        if (packageName !in allowedApps) {
            Log.d(TAG, "⏭️ Skipped - package not in allowedApps")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT) ?: text

        Log.d(TAG, "📋 Parsing - Title: $title | Text: $bigText")

        // Get readable app name
        val appName = try {
            val pm = applicationContext.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            "Unknown App"
        }

        // Use bigText as it contains the full message for longer SMS
        parseTransactionMessage(bigText.ifEmpty { text }, title, appName)
    }

    private fun parseTransactionMessage(message: String, title: String, appName: String) {
        // Use the unified, bulletproof ExpenseParser
        val parsedExpense = ExpenseParser.parse(message)

        if (parsedExpense != null) {
            // Handle Merchant Fallback: Use notification title if parser found "Unknown"
            var merchant = parsedExpense.merchant
            if (merchant == "Unknown Merchant" && title.isNotEmpty()) {
                val genericPatterns = listOf("axis", "hdfc", "icici", "sbi", "bank", "messages", "alert", "truecaller")
                if (!title.contains("OTP", ignoreCase = true) && 
                    !genericPatterns.any { title.contains(it, ignoreCase = true) }) {
                    merchant = title
                }
            }

            Log.i(TAG, "✅ Parsed: Amount='${parsedExpense.amount}', Merchant='$merchant'")
            saveExpense(merchant, parsedExpense.amount, appName)
        } else {
            Log.d(TAG, "⏭️ Message ignored (Spam, OTP, or non-expense)")
        }
    }

    private fun saveExpense(merchant: String, amount: Double, appName: String) {
        serviceScope.launch {
            val predictedCategory = CategoryEngine.predictCategory(merchant)
            val expense = Expense(
                merchant = merchant,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                category = predictedCategory.label,
                groupId = null,
                isAuto = true,
                autoSource = appName
            )

            try {
                val db = ExpenseDatabase.getDatabase(applicationContext)
                db.expenseDao().insert(expense)
                
                // Trigger instant widget update for auto-captured expenses
                WidgetUpdateHelper.updateWidget(applicationContext)
                
                // Haptic feedback for auto-capture
                HapticUtils.playTick(applicationContext)
                
                // Check budget thresholds
                BudgetUtils.checkAndNotifyBudget(applicationContext)

                val intent = Intent("com.context.app.NEW_EXPENSE")
                sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving expense", e)
            }
        }
    }
}
