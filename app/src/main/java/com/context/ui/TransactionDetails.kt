package com.context.ui

import com.context.data.Expense
import java.text.SimpleDateFormat
import java.util.Locale

enum class TransactionType { EXPENSE, CREDIT }
enum class TransactionSource { MANUAL, AUTO_DETECTED }

data class TransactionDetails(
    val merchant: String,
    val dateTime: String,
    val amount: String,
    val type: TransactionType,
    val category: String,
    val source: TransactionSource,
    val isAuto: Boolean,
    val autoSource: String? = null
)

fun Expense.toTransactionDetails(): TransactionDetails {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return TransactionDetails(
        merchant = merchant,
        dateTime = dateFormat.format(java.util.Date(timestamp)),
        amount = String.format(Locale.getDefault(), "%,.2f", amount),
        type = if (category == "Settlement") TransactionType.CREDIT else TransactionType.EXPENSE,
        category = category,
        source = if (isAuto) TransactionSource.AUTO_DETECTED else TransactionSource.MANUAL,
        isAuto = isAuto,
        autoSource = autoSource
    )
}
