package com.context.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.data.RecurringExpense
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ElectricBlue
import com.context.ui.theme.LightBlue
import com.context.utils.*
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val recurringExpenses by homeViewModel.activeRecurringExpenses.collectAsState()
    val allExpenses by homeViewModel.allExpenses.collectAsState()
    val totalMonthlyCommitment = recurringExpenses.sumOf { it.averageAmount }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val privacyMode by remember { com.context.utils.SecurityUtils.getPrivacyModeFlow(context) }.collectAsState(initial = com.context.utils.SecurityUtils.isPrivacyModeEnabled(context))

    val currentMonthRange = remember { DateFilterUtils.getTimeRange(TimeRange.MONTH, Calendar.getInstance()) }
    val thisMonthExpenses = remember(allExpenses, currentMonthRange) {
        allExpenses.filter { it.timestamp in currentMonthRange.first..currentMonthRange.second }
    }

    val handledCount = remember(recurringExpenses, thisMonthExpenses) {
        recurringExpenses.count { recurring ->
            thisMonthExpenses.any { expense ->
                RecurringExpenseDetector.isSameMerchant(expense.merchant, recurring.merchant) &&
                abs(expense.amount - recurring.averageAmount) / recurring.averageAmount < 0.2
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recurring Spends", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // FINANCIAL AUDIT HEADER
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = ElectricBlue)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.verticalGradient(colors = listOf(ElectricBlue, LightBlue)))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                "Monthly Commitment",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val totalText = CurrencyMasker.formatAmount(totalMonthlyCommitment, privacyMode)
                            Text(totalText, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${recurringExpenses.size} active subscriptions",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "$handledCount of ${recurringExpenses.size} paid",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "All Subscriptions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (recurringExpenses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No recurring expenses detected yet.", color = Color.Gray)
                    }
                }
            } else {
                val sortedList = recurringExpenses.sortedWith(
                    compareBy<RecurringExpense> { recurring ->
                        val isPaid = thisMonthExpenses.any { expense ->
                            RecurringExpenseDetector.isSameMerchant(expense.merchant, recurring.merchant) &&
                            abs(expense.amount - recurring.averageAmount) / recurring.averageAmount < 0.2
                        }
                        isPaid // Paid items go to bottom
                    }.thenBy { it.nextExpectedDate }
                )

                items(sortedList) { recurring ->
                    val isPaid = thisMonthExpenses.any { expense ->
                        RecurringExpenseDetector.isSameMerchant(expense.merchant, recurring.merchant) &&
                        abs(expense.amount - recurring.averageAmount) / recurring.averageAmount < 0.2
                    }
                    
                    RecurringManagementRow(
                        recurring = recurring,
                        isPaid = isPaid,
                        isPrivacyMode = privacyMode,
                        onDelete = { homeViewModel.suppressRecurring(recurring.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun RecurringManagementRow(
    recurring: RecurringExpense,
    isPaid: Boolean,
    isPrivacyMode: Boolean,
    onDelete: () -> Unit
) {
    val style = CategoryStyling.getStyle(recurring.category)
    val nextDate = DateUtils.formatDate(recurring.nextExpectedDate)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPaid) 0.dp else 2.dp),
        border = if (isPaid) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isPaid) Color(0xFF2E7D32).copy(alpha = 0.1f) else style.color.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPaid) Icons.Default.CheckCircle else style.icon, 
                        null, 
                        tint = if (isPaid) Color(0xFF2E7D32) else style.boldColor, 
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recurring.merchant.toTitleCase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isPaid) Color(0xFF1B5E20) else Color.Unspecified
                )
                Text(
                    if (isPaid) "Paid for this month" else "Next: $nextDate",
                    fontSize = 12.sp,
                    color = if (isPaid) Color(0xFF2E7D32).copy(alpha = 0.7f) else Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountText = CurrencyMasker.formatSmallAmount(recurring.averageAmount, isPrivacyMode)
                Text(
                    amountText,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = if (isPaid) Color(0xFF1B5E20) else Color.Black
                )
                if (!isPaid) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
