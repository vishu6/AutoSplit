package com.context.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.context.components.DonutChart
import com.context.data.Expense
import com.context.data.Group
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ContextTheme
import com.context.ui.theme.ElectricBlue
import com.context.ui.theme.LightBlue
import com.context.utils.DateUtils
import com.context.utils.OnboardingUtils
import com.context.utils.PermissionUtils
import com.context.utils.TimeRange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToGroup: (Int) -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    onViewAllClick: () -> Unit
) {
    val transactions by homeViewModel.allExpenses.collectAsState(initial = emptyList())
    val groups by homeViewModel.groups.collectAsState(initial = emptyList())
    val filteredTotalSpent by homeViewModel.filteredTotalSpent.collectAsState()
    val filteredExpenses by homeViewModel.filteredExpenses.collectAsState()
    val selectedRange by homeViewModel.selectedTimeRange.collectAsState()
    val currentCalendar by homeViewModel.currentCalendar.collectAsState()

    val spendingExpenses = remember(filteredExpenses) {
        filteredExpenses.filter { it.category != "Settlement" }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var savedName by remember { mutableStateOf(OnboardingUtils.getUserName(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                savedName = OnboardingUtils.getUserName(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!PermissionUtils.isNotificationServiceEnabled(context)) {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { /* Do nothing, force them to choose */ },
            title = { Text("Enable Auto-Tracking") },
            text = { Text("To automatically track expenses from SMS, Split Mate needs 'Notification Access'. Please turn it on in the next screen.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    PermissionUtils.openNotificationSettings(context)
                }) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddExpenseClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    HomeTopBar(name = savedName, onProfileClick = onProfileClick)
                    Spacer(modifier = Modifier.height(24.dp))
                    BalanceSummaryCard(amount = filteredTotalSpent.toString(), selectedRange = selectedRange, calendar = currentCalendar)
                }
            }

            item {
                TimeRangeFilter(
                    selectedRange = selectedRange,
                    onRangeSelected = { homeViewModel.onTimeRangeSelected(it) },
                    calendar = currentCalendar,
                    onNext = { homeViewModel.onNextPeriod() },
                    onPrevious = { homeViewModel.onPreviousPeriod() }
                )
            }

            if (spendingExpenses.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Spend Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(expenses = spendingExpenses, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(24.dp))
                        ChartLegend(expenses = spendingExpenses, modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "My Groups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                GroupsList(
                    groups = groups,
                    expenses = transactions, 
                    onGroupClick = onNavigateToGroup,
                    onNewGroupClick = onCreateGroupClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                val title = when (selectedRange) {
                    TimeRange.TODAY -> {
                        when {
                            DateUtils.isToday(currentCalendar) -> "Today's Transactions"
                            DateUtils.isYesterday(currentCalendar) -> "Yesterday's Transactions"
                            else -> {
                                val day = currentCalendar.get(Calendar.DAY_OF_MONTH)
                                val suffix = DateUtils.getDayOfMonthSuffix(day)
                                val month = SimpleDateFormat("MMM", Locale.getDefault()).format(currentCalendar.time)
                                "$month $day$suffix Transactions"
                            }
                        }
                    }
                    TimeRange.WEEK -> if (DateUtils.isThisWeek(currentCalendar)) "This Week's Transactions" else "Week's Transactions"
                    TimeRange.MONTH -> {
                        if (DateUtils.isThisMonth(currentCalendar)) {
                            "This Month's Transactions"
                        } else {
                            val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
                            "${monthFormat.format(currentCalendar.time)}'s Transactions"
                        }
                    }
                    TimeRange.YEAR -> {
                        if (DateUtils.isThisYear(currentCalendar)) {
                            "This Year's Transactions"
                        } else {
                            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                            "${yearFormat.format(currentCalendar.time)}'s Transactions"
                        }
                    }
                    TimeRange.ALL -> "Recent Transactions"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onViewAllClick) {
                        Text("View All")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(filteredExpenses.take(10)) { expense ->
                    val details = expense.toTransactionDetails()
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onExpenseClick(expense.id) }
                    ) {
                        TransactionItemCard(transaction = details)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun TimeRangeFilter(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    calendar: Calendar,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val dayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    val weekFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    val navigatorLabel = when (selectedRange) {
        TimeRange.TODAY -> dayFormat.format(calendar.time)
        TimeRange.WEEK -> {
            val weekStart = calendar.clone() as Calendar
            weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
            val weekEnd = weekStart.clone() as Calendar
            weekEnd.add(Calendar.DAY_OF_WEEK, 6)
            "${weekFormat.format(weekStart.time)} - ${weekFormat.format(weekEnd.time)}"
        }
        TimeRange.MONTH -> monthYearFormat.format(calendar.time)
        TimeRange.YEAR -> yearFormat.format(calendar.time)
        TimeRange.ALL -> "All Time"
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(TimeRange.TODAY, TimeRange.WEEK, TimeRange.MONTH, TimeRange.YEAR, TimeRange.ALL).forEach { range ->
                TextButton(onClick = { onRangeSelected(range) }) {
                     Text(
                         text = range.name.lowercase().replaceFirstChar { it.uppercase() },
                         fontWeight = if (range == selectedRange) FontWeight.Bold else FontWeight.Normal,
                         color = if (range == selectedRange) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                     )
                }
            }
        }

        if (selectedRange != TimeRange.ALL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous")
                }

                Text(
                    text = navigatorLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next")
                }
            }
        }
    }
}

fun Expense.toTransactionDetails(): TransactionDetails {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return TransactionDetails(
        merchant = this.merchant,
        dateTime = sdf.format(this.timestamp),
        amount = this.amount.toString(),
        type = TransactionType.EXPENSE,
        category = this.category,
        source = if (this.isAuto) TransactionSource.AUTO_DETECTED else TransactionSource.MANUAL,
        isAuto = this.isAuto
    )
}

@Composable
private fun HomeTopBar(name: String, onProfileClick: () -> Unit) {
    val greetingTime = remember { DateUtils.getGreeting() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$greetingTime, $name",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Image(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onProfileClick() }
                .padding(8.dp)
        )
    }
}

@Composable
private fun BalanceSummaryCard(amount: String, selectedRange: TimeRange, calendar: Calendar) {
    val title = when (selectedRange) {
        TimeRange.TODAY -> {
            when {
                DateUtils.isToday(calendar) -> "Total Spent Today"
                DateUtils.isYesterday(calendar) -> "Total Spent Yesterday"
                else -> {
                    val monthDayFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
                    "Total Spent on ${monthDayFormat.format(calendar.time)}"
                }
            }
        }
        TimeRange.WEEK -> if (DateUtils.isThisWeek(calendar)) "Total Spent This Week" else "Total Spent in Week"
        TimeRange.MONTH -> {
            if (DateUtils.isThisMonth(calendar)) {
                "Total Spent This Month"
            } else {
                val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
                "Total Spent in ${monthFormat.format(calendar.time)}"
            }
        }
        TimeRange.YEAR -> {
            if (DateUtils.isThisYear(calendar)) {
                "Total Spent This Year"
            } else {
                val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                "Total Spent in ${yearFormat.format(calendar.time)}"
            }
        }
        TimeRange.ALL -> "Total Spent All Time"
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ElectricBlue, LightBlue)
                    )
                )
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "₹$amount",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ChartLegend(expenses: List<Expense>, modifier: Modifier = Modifier) {
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    val totalAmount = categoryTotals.values.sum()

    Column(modifier = modifier) {
        categoryTotals.forEach { (category, amount) ->
            val percentage = (amount / totalAmount * 100).toFloat()
            val style = CategoryStyling.getStyle(category)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(style.color, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${String.format("%.1f", percentage)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GroupsList(groups: List<Group>, expenses: List<Expense>, onGroupClick: (Int) -> Unit, onNewGroupClick: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            NewGroupCard(onClick = onNewGroupClick)
        }
        items(groups) { group ->
            val groupExpenses = expenses.filter { it.groupId == group.groupId }
            val groupTotal = groupExpenses
                .filter { it.category != "Settlement" } 
                .sumOf { it.amount }

            GroupCard(
                groupName = group.name,
                totalAmount = groupTotal, 
                onClick = { onGroupClick(group.groupId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewGroupCard(onClick: () -> Unit) {
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    Card(onClick = onClick, modifier = Modifier.height(140.dp).width(120.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()){
                drawRoundRect(color = Color.LightGray, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Add, contentDescription = "New Group", tint = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("New Group", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupCard(groupName: String, totalAmount: Double, onClick: () -> Unit) {
    val pastelColors = listOf(
        Color(0xFFE3F2FD), // Pastel Blue
        Color(0xFFF3E5F5), // Pastel Purple
        Color(0xFFFFF0E5), // Pastel Orange
        Color(0xFFE8F5E9)  // Pastel Green
    )
    val cardColor = pastelColors[groupName.hashCode() % pastelColors.size]

    Card(
        modifier = Modifier
            .height(140.dp)
            .width(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = groupName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "₹${totalAmount.toInt()}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Empty",
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Waiting for new expenses...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
fun HomeScreenWithGroupsPreview() {
    ContextTheme {
        HomeScreen(
            homeViewModel = FakeHomeViewModelFactory.create(),
            onNavigateToGroup = {}, 
            onCreateGroupClick = {}, 
            onAddExpenseClick = {}, 
            onExpenseClick = {}, 
            onProfileClick = {}, 
            onViewAllClick = {}
        )
    }
}
