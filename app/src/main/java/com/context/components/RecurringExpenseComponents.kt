package com.context.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.data.Expense
import com.context.data.RecurringExpense
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ElectricBlue
import com.context.utils.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun RecurringSection(
    recurringExpenses: List<RecurringExpense>,
    expenses: List<Expense>,
    isPrivacyMode: Boolean,
    onViewAllClick: () -> Unit,
    onMarkAsPaid: (Int) -> Unit = {},
    onDismissSuggestion: (Int) -> Unit = {}
) {
    if (recurringExpenses.isEmpty()) return

    val currentMonthRange = remember { DateFilterUtils.getTimeRange(TimeRange.MONTH, Calendar.getInstance()) }
    val thisMonthExpenses = remember(expenses, currentMonthRange) {
        expenses.filter { it.timestamp in currentMonthRange.first..currentMonthRange.second }
    }

    // SMART FILTER LOGIC:
    val filteredForDashboard = remember(recurringExpenses, thisMonthExpenses) {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L

        recurringExpenses.filter { recurring ->
            val matchedExpense = thisMonthExpenses.find { expense ->
                RecurringExpenseDetector.isSameMerchant(expense.merchant, recurring.merchant) &&
                abs(expense.amount - recurring.averageAmount) / recurring.averageAmount < 0.2
            }
            
            val isPaid = matchedExpense != null
            val isPaidRecently = matchedExpense?.let { (now - it.timestamp) < oneDayMillis } ?: false
            val isDueSoon = (recurring.nextExpectedDate - now) < sevenDaysMillis
            val isOverdue = recurring.nextExpectedDate < now && !isPaid

            isOverdue || (isDueSoon && !isPaid) || isPaidRecently || !recurring.isActive
        }.sortedBy { it.nextExpectedDate }
    }

    if (filteredForDashboard.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recurring This Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "View All",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
        ) {
            Column {
                filteredForDashboard.take(4).forEachIndexed { index, recurring ->
                    val matchedExpense = thisMonthExpenses.find { expense ->
                        RecurringExpenseDetector.isSameMerchant(expense.merchant, recurring.merchant) &&
                        abs(expense.amount - recurring.averageAmount) / recurring.averageAmount < 0.2
                    }
                    val isPaid = matchedExpense != null

                    if (recurring.isActive) {
                        CompactRecurringRow(
                            recurring = recurring,
                            isPaid = isPaid,
                            isPrivacyMode = isPrivacyMode
                        )
                    } else if (!recurring.isSuppressed) {
                        RecurringSuggestionRow(
                            recurring = recurring,
                            onAccept = { onMarkAsPaid(recurring.id) },
                            onDismiss = { onDismissSuggestion(recurring.id) }
                        )
                    }

                    if (index < filteredForDashboard.size - 1 && index < 3) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactRecurringRow(
    recurring: RecurringExpense,
    isPaid: Boolean,
    isPrivacyMode: Boolean
) {
    val style = CategoryStyling.getStyle(recurring.category)
    val diffMillis = recurring.nextExpectedDate - System.currentTimeMillis()
    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
    
    val statusText = if (isPaid) "Paid" else when {
        days < 0 -> "Overdue ${-days}d"
        days == 0L -> "Due Today"
        else -> "Due in ${days}d"
    }
    
    val statusColor = if (isPaid) Color(0xFF2E7D32) else if (days < 0) Color.Red else ElectricBlue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = style.color.copy(alpha = 0.1f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(style.icon, null, tint = style.boldColor, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // Global Formatting: Title Case for merchant
            Text(
                recurring.merchant.toTitleCase(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                statusText,
                fontSize = 11.sp,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Global Formatting: Comma separators for amount
        val amountText = CurrencyMasker.formatSmallAmount(recurring.averageAmount, isPrivacyMode)
        Text(
            amountText, 
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        
        if (isPaid) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.CheckCircle, 
                null, 
                tint = Color(0xFF2E7D32), 
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun RecurringSuggestionRow(
    recurring: RecurringExpense,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElectricBlue.copy(alpha = 0.05f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.NotificationsActive, null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Global Formatting: Title Case for merchant
            Text(
                "Detect ${recurring.merchant.toTitleCase()}?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricBlue
            )
            Text("Suggested recurring expense", fontSize = 11.sp, color = Color.Gray)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onAccept,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Yes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}
