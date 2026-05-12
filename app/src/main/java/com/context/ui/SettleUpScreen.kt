package com.context.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.HapticUtils
import com.context.utils.ReviewManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    groupId: Int,
    onBack: () -> Unit,
    onSettled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ExpenseDatabase.getDatabase(context) }

    var amount by remember { mutableStateOf("") }
    var payerName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("How much did you receive?", color = Color.Gray)
            
            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₹", style = MaterialTheme.typography.displayMedium) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Payer Input
            OutlinedTextField(
                value = payerName,
                onValueChange = { payerName = it },
                label = { Text("Who paid you?") },
                placeholder = { Text("e.g. Rahul, Dad, Alice") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (amountVal > 0 && payerName.isNotBlank()) {
                        scope.launch {
                            db.expenseDao().insert(
                                Expense(
                                    merchant = "Payment from $payerName",
                                    amount = amountVal,
                                    timestamp = System.currentTimeMillis(),
                                    category = "Settlement",
                                    groupId = groupId,
                                    isAuto = false
                                )
                            )
                            db.expenseDao().recalculateGroupTotal(groupId)
                            
                            // Haptic feedback for settlement (Satisfying thud)
                            HapticUtils.playThud(context)
                            
                            // CHECK FOR REVIEW ELIGIBILITY
                            if (ReviewManager.shouldShowReview(context)) {
                                context.findActivity()?.let { activity ->
                                    ReviewManager.launchReviewFlow(activity)
                                }
                            }
                            
                            onSettled()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Record Payment")
            }
        }
    }
}

// Helper to find Activity from Context
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
