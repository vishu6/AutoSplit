package com.context.utils

import android.content.Context
import android.content.Intent
import com.context.data.Expense

object ShareUtils {

    fun shareGroupSummary(context: Context, groupName: String, total: Double, expenses: List<Expense>) {
        // 1. Build the "Magic String"
        val sb = StringBuilder()
        sb.append("🧾 *Bill Summary: $groupName*\n")
        sb.append("----------------\n")
        sb.append("💰 *Total Spent: ₹${String.format("%.2f", total)}*\n\n")
        
        sb.append("👇 Recent Expenses:\n")
        // Show last 5 expenses
        expenses.take(5).forEach { expense ->
            sb.append("• ${expense.merchant}: ₹${expense.amount} (${expense.paidBy})\n")
        }
        
        if (expenses.size > 5) {
            sb.append("...and ${expenses.size - 5} more.\n")
        }
        
        sb.append("\n----------------\n")
        sb.append("⚡ *Tracked via Context App*")

        // 2. Launch Android Share Sheet
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share Bill via...")
        context.startActivity(shareIntent)
    }
}
