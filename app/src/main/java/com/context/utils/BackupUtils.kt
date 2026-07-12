package com.context.utils

import android.content.Context
import android.net.Uri
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupUtils {

    suspend fun exportToCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = ExpenseDatabase.getDatabase(context)
            val expenses = db.expenseDao().getAllExpenses().first()
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // Header
                    writer.write("Merchant,Amount,Category,Date,PaidBy,IsAuto,Source\n")
                    
                    expenses.forEach { expense ->
                        val date = DateUtils.formatDate(expense.timestamp)
                        writer.write("\"${expense.merchant}\",${expense.amount},\"${expense.category}\",\"$date\",\"${expense.paidBy}\",${expense.isAuto},\"${expense.autoSource ?: ""}\"\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromCsv(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val db = ExpenseDatabase.getDatabase(context)
            var count = 0
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header
                    reader.readLine()
                    
                    var line: String? = reader.readLine()
                    val importedExpenses = mutableListOf<Expense>()
                    
                    while (line != null) {
                        val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                        if (parts.size >= 5) {
                            val merchant = parts[0].replace("\"", "")
                            val amount = parts[1].toDoubleOrNull() ?: 0.0
                            val category = parts[2].replace("\"", "")
                            val dateStr = parts[3].replace("\"", "")
                            val paidBy = parts[4].replace("\"", "")
                            
                            // Basic date parsing - in a real app, use a more robust parser
                            val timestamp = System.currentTimeMillis() 

                            importedExpenses.add(
                                Expense(
                                    merchant = merchant,
                                    amount = amount,
                                    category = category,
                                    timestamp = timestamp,
                                    paidBy = paidBy
                                )
                            )
                            count++
                        }
                        line = reader.readLine()
                    }
                    
                    if (importedExpenses.isNotEmpty()) {
                        db.expenseDao().insertAll(importedExpenses)
                    }
                }
            }
            count
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
