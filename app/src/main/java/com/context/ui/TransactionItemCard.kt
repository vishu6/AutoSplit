package com.context.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ContextTheme
import com.context.ui.theme.FintechGreen
import com.context.ui.theme.FintechRed

// --- DATA ---
enum class TransactionType { EXPENSE, CREDIT }
enum class TransactionSource { MANUAL, AUTO_DETECTED }

data class TransactionDetails(
    val merchant: String,
    val dateTime: String,
    val amount: String,
    val type: TransactionType,
    val category: String, // Updated to String
    val source: TransactionSource
)

// --- COMPOSABLE ---

@Composable
fun TransactionItemCard(transaction: TransactionDetails) {
    val amountColor = if (transaction.type == TransactionType.EXPENSE) FintechRed else FintechGreen
    val amountPrefix = if (transaction.type == TransactionType.EXPENSE) "-" else "+"

    // Get style dynamically from the CategoryStyling object
    val categoryStyle = CategoryStyling.getStyle(transaction.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The Category Icon Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = categoryStyle.color, // Dynamic Color
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = categoryStyle.icon, // Dynamic Icon
                    contentDescription = transaction.category,
                    tint = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Center: Merchant and Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = transaction.dateTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Side: Amount and Chip
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix₹${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    fontSize = 16.sp
                )
                if (transaction.source == TransactionSource.AUTO_DETECTED) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Auto-Detected",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
fun TransactionItemCardPreview() {
    val foodExpense = TransactionDetails(
        merchant = "Zomato",
        dateTime = "Today, 10:30 AM",
        amount = "250",
        type = TransactionType.EXPENSE,
        category = "Food & Drink", // Using String
        source = TransactionSource.AUTO_DETECTED
    )

    val travelCredit = TransactionDetails(
        merchant = "Uber",
        dateTime = "Yesterday, 5:45 PM",
        amount = "210",
        type = TransactionType.CREDIT,
        category = "Transport", // Using String
        source = TransactionSource.MANUAL
    )
    
    ContextTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TransactionItemCard(transaction = foodExpense)
            TransactionItemCard(transaction = travelCredit)
        }
    }
}
