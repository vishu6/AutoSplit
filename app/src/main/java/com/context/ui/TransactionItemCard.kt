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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.context.ui.theme.CategoryStyle
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ContextTheme

enum class TransactionType { EXPENSE, CREDIT }
enum class TransactionSource { MANUAL, AUTO_DETECTED }

data class TransactionDetails(
    val merchant: String,
    val dateTime: String,
    val amount: String,
    val type: TransactionType,
    val category: String,
    val source: TransactionSource,
    val isAuto: Boolean // Add isAuto to the UI model
)

@Composable
fun TransactionItemCard(transaction: TransactionDetails) {
    val isSettlement = transaction.category == "Settlement"

    val categoryStyle = if (isSettlement) {
        CategoryStyle(color = Color(0xFFE8F5E9), icon = Icons.Default.CheckCircle) // Green theme
    } else {
        CategoryStyling.getStyle(transaction.category)
    }

    val amountColor = if (isSettlement) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSettlement) categoryStyle.color else categoryStyle.color.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = transaction.category,
                    tint = if (isSettlement) Color(0xFF2E7D32) else categoryStyle.color,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = transaction.merchant, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isSettlement) "Repayment" else transaction.dateTime, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isSettlement) "+ " else "") + "₹${transaction.amount}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (transaction.source == TransactionSource.AUTO_DETECTED && !isSettlement) {
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

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
fun TransactionItemCardPreview() {
    val foodExpense = TransactionDetails(
        merchant = "Zomato",
        dateTime = "Today, 10:30 AM",
        amount = "250",
        type = TransactionType.EXPENSE,
        category = "Food & Drink",
        source = TransactionSource.AUTO_DETECTED,
        isAuto = true
    )

    val settlement = TransactionDetails(
        merchant = "Payment from Alex",
        dateTime = "Yesterday, 5:45 PM",
        amount = "500",
        type = TransactionType.CREDIT, // This can be simplified
        category = "Settlement",
        source = TransactionSource.MANUAL,
        isAuto = false
    )
    
    ContextTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TransactionItemCard(transaction = foodExpense)
            TransactionItemCard(transaction = settlement)
        }
    }
}
