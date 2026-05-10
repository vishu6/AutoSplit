package com.context.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.context.components.DonutChart
import com.context.components.UpdateAvailableBanner
import com.context.data.Expense
import com.context.data.Group
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ContextTheme
import com.context.ui.theme.ElectricBlue
import com.context.ui.theme.LightBlue
import com.context.ui.theme.FintechRed
import com.context.utils.DateUtils
import com.context.utils.OnboardingUtils
import com.context.utils.PermissionUtils
import com.context.utils.TimeRange
import com.context.utils.UpdateUtils
import com.context.utils.HapticUtils
import com.context.utils.BudgetUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToGroup: (Int) -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onSetBudgetClick: () -> Unit
) {
    val transactions by homeViewModel.allExpenses.collectAsState(initial = emptyList())
    val groups by homeViewModel.groups.collectAsState(initial = emptyList())
    val filteredTotalSpent by homeViewModel.filteredTotalSpent.collectAsState()
    val previousPeriodTotalSpent by homeViewModel.previousPeriodTotalSpent.collectAsState()
    val filteredExpenses by homeViewModel.filteredExpenses.collectAsState()
    val selectedRange by homeViewModel.selectedTimeRange.collectAsState()
    val currentCalendar by homeViewModel.currentCalendar.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val isSearchActive by homeViewModel.isSearchActive.collectAsState()

    val spendingExpenses = remember(filteredExpenses) {
        filteredExpenses.filter { it.category != "Settlement" }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var savedName by remember { mutableStateOf(OnboardingUtils.getUserName(context)) }
    
    // Budget State
    var isBudgetSet by remember { mutableStateOf(BudgetUtils.isBudgetSet(context)) }
    var monthlyBudgetValue by remember { mutableStateOf(BudgetUtils.getMonthlyBudget(context)) }

    // FAB State
    var fabExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive) {
        homeViewModel.setSearchActive(false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                savedName = OnboardingUtils.getUserName(context)
                isBudgetSet = BudgetUtils.isBudgetSet(context)
                monthlyBudgetValue = BudgetUtils.getMonthlyBudget(context)
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

    // Haptic for search results
    LaunchedEffect(filteredExpenses.size) {
        if (isSearchActive && filteredExpenses.isNotEmpty()) {
            HapticUtils.playTick(context)
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { /* Do nothing */ },
            title = { Text("Enable Auto-Tracking") },
            text = { Text("To automatically track expenses from SMS, Cleave needs 'Notification Access'.") },
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.systemBars 
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (UpdateUtils.showUpdateBanner && !isSearchActive) {
                    item {
                        UpdateAvailableBanner(onUpdateClick = onUpdateClick)
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        HomeTopBar(
                            name = savedName,
                            onProfileClick = onProfileClick,
                            isSearchActive = isSearchActive,
                            onSearchClick = { 
                                HapticUtils.playTick(context)
                                homeViewModel.setSearchActive(true) 
                            },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { homeViewModel.onSearchQueryChanged(it) },
                            onClearSearch = { homeViewModel.setSearchActive(false) }
                        )

                        AnimatedVisibility(
                            visible = !isSearchActive,
                            enter = fadeIn(animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(24.dp))
                                BalanceSummaryCard(
                                    currentAmount = filteredTotalSpent,
                                    previousAmount = previousPeriodTotalSpent,
                                    selectedRange = selectedRange,
                                    calendar = currentCalendar,
                                    monthlyBudget = if (isBudgetSet) monthlyBudgetValue else null
                                )
                                
                                if (!isBudgetSet && selectedRange == TimeRange.MONTH && DateUtils.isThisMonth(currentCalendar)) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    BudgetNudgeBanner(onSetBudgetClick = onSetBudgetClick)
                                }
                            }
                        }
                    }
                }

                if (!isSearchActive) {
                    item {
                        TimeRangeFilter(
                            selectedRange = selectedRange,
                            onRangeSelected = { 
                                HapticUtils.playTick(context)
                                homeViewModel.onTimeRangeSelected(it) 
                            },
                            calendar = currentCalendar,
                            onNext = { 
                                HapticUtils.playTick(context)
                                homeViewModel.onNextPeriod() 
                            },
                            onPrevious = { 
                                HapticUtils.playTick(context)
                                homeViewModel.onPreviousPeriod() 
                            }
                        )
                    }

                    if (spendingExpenses.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Spend Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        GroupsList(
                            groups = groups,
                            expenses = transactions, 
                            onGroupClick = { groupId ->
                                HapticUtils.playTick(context)
                                onNavigateToGroup(groupId)
                            },
                            onNewGroupClick = {
                                HapticUtils.playTick(context)
                                onCreateGroupClick()
                            }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                item {
                    val title = if (isSearchActive) {
                        if (searchQuery.isEmpty()) "Recent Transactions" else "Search Results"
                    } else {
                        when (selectedRange) {
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!isSearchActive) {
                            TextButton(onClick = onViewAllClick) {
                                Text("View All")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (filteredExpenses.isEmpty()) {
                    item {
                        EmptyState(isSearching = isSearchActive)
                    }
                } else {
                    items(if (isSearchActive) filteredExpenses else filteredExpenses.take(10)) { expense ->
                        val details = expense.toTransactionDetails()
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .combinedClickable(
                                    onClick = { onExpenseClick(expense.id) },
                                    onLongClick = {
                                        HapticUtils.playHeavyClick(context)
                                        onExpenseClick(expense.id)
                                    }
                                )
                        ) {
                            TransactionItemCard(transaction = details)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // SCRIM
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { fabExpanded = false }
            )
        }

        // FAB
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            ExpandableFab(
                isExpanded = fabExpanded,
                onMainFabClick = { 
                    HapticUtils.playTick(context)
                    fabExpanded = !fabExpanded 
                },
                onManualEntryClick = {
                    HapticUtils.playTick(context)
                    fabExpanded = false
                    onAddExpenseClick()
                },
                onScanReceiptClick = {
                    HapticUtils.playTick(context)
                    fabExpanded = false
                    onScanReceiptClick()
                }
            )
        }
    }
}

@Composable
fun BudgetNudgeBanner(onSetBudgetClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().clickable { onSetBudgetClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "No budget set yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Know when you're overspending before it's too late.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Set Monthly Budget →",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { 
            Text(
                text = "Search transactions...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            ) 
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue) },
        trailingIcon = {
            IconButton(onClick = onClearSearch) {
                Icon(Icons.Default.Close, contentDescription = "Close Search")
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        maxLines = 1,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricBlue,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun ExpandableFab(
    isExpanded: Boolean,
    onMainFabClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onScanReceiptClick: () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 45f else 0f, label = "Rotate")

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = onManualEntryClick,
                    containerColor = Color.White,
                    contentColor = ElectricBlue,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    text = { Text("Manual Entry", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExtendedFloatingActionButton(
                    onClick = onScanReceiptClick,
                    containerColor = Color.White,
                    contentColor = ElectricBlue,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    text = { Text("Scan Receipt", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        FloatingActionButton(
            onClick = onMainFabClick,
            containerColor = if (isExpanded) Color.White else MaterialTheme.colorScheme.primary,
            contentColor = if (isExpanded) ElectricBlue else Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 12.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Menu",
                modifier = Modifier.rotate(rotation)
            )
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
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurface)
                }

                Text(
                    text = navigatorLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
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
        amount = String.format("%.2f", this.amount),
        type = TransactionType.EXPENSE,
        category = this.category,
        source = if (this.isAuto) TransactionSource.AUTO_DETECTED else TransactionSource.MANUAL,
        isAuto = this.isAuto
    )
}

@Composable
private fun HomeTopBar(
    name: String, 
    onProfileClick: () -> Unit,
    isSearchActive: Boolean,
    onSearchClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    val greetingTime = remember { DateUtils.getGreeting() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (!isSearchActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$greetingTime, $name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Image(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onProfileClick() }
                            .padding(8.dp)
                    )
                }
            }
        } else {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BalanceSummaryCard(
    currentAmount: Double,
    previousAmount: Double,
    selectedRange: TimeRange,
    calendar: Calendar,
    monthlyBudget: Double? = null
) {
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

    val comparisonText: Pair<String, Color?>? = remember(currentAmount, previousAmount, selectedRange, calendar) {
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val isCurrentMonth = DateUtils.isThisMonth(calendar)
        
        if (selectedRange == TimeRange.MONTH && isCurrentMonth && dayOfMonth <= 3) {
            if (previousAmount > 0) {
                Pair("Last month total: ₹${String.format("%,.0f", previousAmount)}", Color.White.copy(alpha = 0.8f))
            } else null
        } else if (previousAmount == 0.0) {
            if (currentAmount > 0) Pair("Start your savings journey! 🚀", Color.White.copy(alpha = 0.8f)) else null
        } else {
            val difference = currentAmount - previousAmount
            val absDiff = abs(difference)
            val formattedDiff = String.format("%,.0f", absDiff)
            
            when {
                difference < 0 -> Pair("📉 ₹$formattedDiff less than last period", Color.White)
                difference > 0 -> Pair("📈 ₹$formattedDiff more than last period", Color.White)
                else -> Pair("Same as last period", Color.White.copy(alpha = 0.8f))
            }
        }
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
                    text = "₹${String.format("%,.2f", currentAmount)}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                // Progress Bar for Budget
                if (monthlyBudget != null && selectedRange == TimeRange.MONTH && DateUtils.isThisMonth(calendar)) {
                    val progress = (currentAmount / monthlyBudget).coerceIn(0.0, 1.0).toFloat()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress > 0.9f) FintechRed else Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "₹${String.format("%,.0f", currentAmount)} of ₹${String.format("%,.0f", monthlyBudget)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (comparisonText != null && selectedRange != TimeRange.ALL && (monthlyBudget == null || selectedRange != TimeRange.MONTH)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = comparisonText.first,
                        color = comparisonText.second ?: Color.White,
                        style = MaterialTheme.typography.bodySmall)
                }
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
                Text(category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                Text("${String.format("%.1f", percentage)}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
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
    val onSurface = MaterialTheme.colorScheme.onSurface
    Card(onClick = onClick, modifier = Modifier.height(140.dp).width(120.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()){
                drawRoundRect(color = onSurface.copy(alpha = 0.3f), style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Add, contentDescription = "New Group", tint = onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("New Group", color = onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupCard(groupName: String, totalAmount: Double, onClick: () -> Unit) {
    val pastelColors = listOf(
        Color(0xFFE3F2FD),
        Color(0xFFF3E5F5),
        Color(0xFFFFF0E5),
        Color(0xFFE8F5E9)
    )
    
    val cardColor = if (pastelColors.isNotEmpty()) {
        val index = abs(groupName.hashCode() % pastelColors.size)
        pastelColors[index]
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

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
                color = Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "₹${totalAmount.toInt()}",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(
            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.AccountBalanceWallet,
            contentDescription = "Empty",
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No transactions found matching your search." else "No transactions yet. Tap the '+' button to add your first one!",
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
            onViewAllClick = {},
            onUpdateClick = {},
            onScanReceiptClick = {},
            onSetBudgetClick = {}
        )
    }
}
