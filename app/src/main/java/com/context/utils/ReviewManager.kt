package com.context.utils

import android.app.Activity
import android.content.Context
import com.context.data.ExpenseDatabase
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.TimeUnit

object ReviewManager {
    private const val PREF_NAME = "review_prefs"
    private const val KEY_FIRST_LAUNCH = "first_launch_time"
    private const val KEY_LAST_PROMPT = "last_prompt_time"
    private const val KEY_PROMPT_COUNT = "prompt_count"

    private fun getPrefs(context: Context) = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun init(context: Context) {
        val prefs = getPrefs(context)
        if (prefs.getLong(KEY_FIRST_LAUNCH, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
    }

    suspend fun shouldShowReview(context: Context): Boolean {
        val prefs = getPrefs(context)
        val db = ExpenseDatabase.getDatabase(context)
        
        // 1. Transaction Count check (10+)
        val txCount = db.expenseDao().getTransactionCount()
        if (txCount < 10) return false

        // 2. Days since first launch (7+)
        val firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
        val daysSinceLaunch = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - firstLaunch)
        if (daysSinceLaunch < 7) return false

        // 3. Prompted before / Frequency (3 months check)
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0L)
        val threeMonthsInMillis = TimeUnit.DAYS.toMillis(90)
        if (System.currentTimeMillis() - lastPrompt < threeMonthsInMillis) return false

        return true
    }

    fun launchReviewFlow(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown.
                    // We record the time to respect the 3-month frequency.
                    getPrefs(activity).edit()
                        .putLong(KEY_LAST_PROMPT, System.currentTimeMillis())
                        .putInt(KEY_PROMPT_COUNT, getPrefs(activity).getInt(KEY_PROMPT_COUNT, 0) + 1)
                        .apply()
                }
            }
        }
    }
}
