package com.example.context.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Receipt
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

// --- DATA ---

enum class TransactionType { EXPENSE, CREDIT }
enum class TransactionSource { MANUAL, AUTO_DETECTED }
enum class Category { FOOD, TRAVEL, OTHER }

data class TransactionDetails(
    val merchant: String,
    val dateTime: String,
    val amount: String,
    val type: TransactionType,
    val category: Category,
    val source: TransactionSource
)

// --- COMPOSABLE ---

@Composable
fun TransactionItemCard(transaction: TransactionDetails) {
    val FintechRed = Color(0xFFE53935)
    val FintechGreen = Color(0xFF43A047)
    val amountColor = if (transaction.type == TransactionType.EXPENSE) FintechRed else FintechGreen
    val amountPrefix = if (transaction.type == TransactionType.EXPENSE) "-" else "+"

    val (categoryIcon, categoryBgColor) = when (transaction.category) {
        Category.FOOD -> Pair(Icons.Default.Fastfood, Color(0xFFFFF0E5)) // Pastel Orange
        Category.TRAVEL -> Pair(Icons.Default.DirectionsCar, Color(0xFFE3F2FD)) // Pastel Blue
        else -> Pair(Icons.Default.Receipt, Color(0xFFF3E5F5)) // Pastel Purple
    }

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
            // Left Side: Icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = categoryBgColor,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category.name,
                    tint = MaterialTheme.colorScheme.primary,
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
                    Spacer(modifier = Modifier.padding(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Text(
                            text = "Auto-Detected",
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true)
@Composable
fun TransactionItemCardPreview() {
    val foodExpense = TransactionDetails(
        merchant = "Starbucks",
        dateTime = "Today, 10:30 AM",
        amount = "450",
        type = TransactionType.EXPENSE,
        category = Category.FOOD,
        source = TransactionSource.AUTO_DETECTED
    )

    val travelCredit = TransactionDetails(
        merchant = "Uber Refund",
        dateTime = "Yesterday, 5:45 PM",
        amount = "210",
        type = TransactionType.CREDIT,
        category = Category.TRAVEL,
        source = TransactionSource.MANUAL
    )

    ContextTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TransactionItemCard(transaction = foodExpense)
            TransactionItemCard(transaction = travelCredit)
        }
    }
}
