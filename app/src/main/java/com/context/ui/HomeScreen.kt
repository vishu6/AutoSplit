package com.context.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.context.components.*
import com.context.data.Expense
import com.context.data.Group
import com.context.ui.theme.CategoryStyle
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ElectricBlue
import com.context.ui.theme.LightBlue
import com.context.utils.*
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToGroup: (Int) -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onViewAllRecurringClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onSetBudgetClick: () -> Unit
) {
    val transactions by homeViewModel.allExpenses.collectAsState(initial = emptyList())
    val groups by homeViewModel.groups.collectAsState(initial = emptyList())
    val filteredTotalSpent by homeViewModel.filteredTotalSpent.collectAsState()
    val monthlyTotalSpent by homeViewModel.currentMonthTotalSpent.collectAsState()
    val filteredExpenses by homeViewModel.filteredExpenses.collectAsState()
    val selectedRange by homeViewModel.selectedTimeRange.collectAsState()
    val currentCalendar by homeViewModel.currentCalendar.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val isSearchActive by homeViewModel.isSearchActive.collectAsState()

    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val trendData by homeViewModel.categoryTrendData.collectAsState()
    val categoryLimits by homeViewModel.categoryLimits.collectAsState()
    val categorySpentMap by homeViewModel.currentMonthCategorySpent.collectAsState()
    
    val categoryMap by homeViewModel.categoryMap.collectAsState()
    val activeRecurringExpenses by homeViewModel.activeRecurringExpenses.collectAsState()

    val context = LocalContext.current
    val privacyMode by remember { SecurityUtils.getPrivacyModeFlow(context) }.collectAsState(initial = SecurityUtils.isPrivacyModeEnabled(context))

    val spendingExpenses = remember(filteredExpenses) {
        filteredExpenses.filter { it.category != "Settlement" }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val toaster = LocalToaster.current
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showBatteryExplanationDialog by remember { mutableStateOf(false) }
    var showPermissionBanner by remember { mutableStateOf(false) }
    var showBatteryBanner by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showRecurringDialog by remember { mutableStateOf<Int?>(null) }

    var savedName by remember { mutableStateOf(OnboardingUtils.getUserName(context)) }
    var currentGreeting by remember { mutableStateOf(DateUtils.getGreeting()) }
    var isBudgetSet by remember { mutableStateOf(BudgetUtils.isBudgetSet(context)) }
    var monthlyBudgetValue by remember { mutableDoubleStateOf(BudgetUtils.getMonthlyBudget(context)) }

    var fabExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCategoryDetail by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != null) {
            showCategoryDetail = true
        }
    }

    BackHandler(enabled = isSearchActive) {
        homeViewModel.setSearchActive(false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // SAFETY RESET: Home screen doesn't support custom range picker, 
                // so reset to Month if user returns from AllTransactions with a custom filter.
                if (homeViewModel.selectedTimeRange.value == TimeRange.CUSTOM) {
                    homeViewModel.onTimeRangeSelected(TimeRange.MONTH)
                }

                savedName = OnboardingUtils.getUserName(context)
                currentGreeting = DateUtils.getGreeting()
                isBudgetSet = BudgetUtils.isBudgetSet(context)
                monthlyBudgetValue = BudgetUtils.getMonthlyBudget(context)
                homeViewModel.refreshBudgetLimits()
                
                val notificationEnabled = PermissionUtils.isNotificationServiceEnabled(context)
                if (!notificationEnabled && !PermissionUtils.isPermanentDismissed(context)) {
                    val count = PermissionUtils.getNudgeCount(context)
                    val lastNudge = PermissionUtils.getLastNudgeTimestamp(context)
                    val diff = System.currentTimeMillis() - lastNudge
                    val days = TimeUnit.MILLISECONDS.toDays(diff)

                    when {
                        count == 0 -> showPermissionDialog = true
                        count < 3 && days >= 7L -> showPermissionDialog = true
                        count < 3 -> showPermissionBanner = !PermissionUtils.isBannerDismissedForCurrentCount(context)
                        else -> { showPermissionDialog = false; showPermissionBanner = false }
                    }
                } else {
                    showPermissionDialog = false; showPermissionBanner = false
                    if (notificationEnabled) {
                        showBatteryBanner = !PermissionUtils.isBatteryOptimizationIgnored(context) && 
                                           !PermissionUtils.isBatteryBannerDismissed(context)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.systemBars 
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (UpdateUtils.showUpdateBanner && !isSearchActive) item { UpdateAvailableBanner(onUpdateClick = onUpdateClick) }
                
                if (showPermissionBanner && !isSearchActive) item { PermissionNudgeBanner(onEnable = { showPermissionDialog = true }, onDismiss = { PermissionUtils.setBannerDismissed(context); showPermissionBanner = false }) }
                if (showBatteryBanner && !isSearchActive) item { BatteryOptimizationNudgeBanner(onFix = { showBatteryExplanationDialog = true }, onDismiss = { PermissionUtils.setBatteryBannerDismissed(context, true); showBatteryBanner = false }) }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        HomeTopBar(
                            name = savedName,
                            greeting = currentGreeting,
                            onProfileClick = onProfileClick,
                            isSearchActive = isSearchActive,
                            onSearchClick = { HapticUtils.playTick(context); homeViewModel.setSearchActive(true) },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { homeViewModel.onSearchQueryChanged(it) },
                            onClearSearch = { homeViewModel.setSearchActive(false) },
                            isPrivacyMode = privacyMode,
                            onPrivacyToggle = {
                                HapticUtils.playTick(context)
                                SecurityUtils.setPrivacyModeEnabled(context, !privacyMode)
                                WidgetUpdateHelper.updateWidget(context, wait = false)
                            }
                        )

                        AnimatedVisibility(visible = !isSearchActive, enter = fadeIn(), exit = fadeOut()) {
                            Column {
                                Spacer(modifier = Modifier.height(24.dp))
                                BalanceSummaryCard(
                                    currentAmount = filteredTotalSpent,
                                    monthlyTotalSpent = monthlyTotalSpent,
                                    selectedRange = selectedRange,
                                    calendar = currentCalendar,
                                    monthlyBudget = if (isBudgetSet) monthlyBudgetValue else null,
                                    isPrivacyMode = privacyMode
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                ModernTimeRangeFilter(
                                    selectedRange = selectedRange,
                                    onRangeSelected = { HapticUtils.playTick(context); homeViewModel.onTimeRangeSelected(it) },
                                    calendar = currentCalendar,
                                    onNext = { HapticUtils.playTick(context); homeViewModel.onNextPeriod() },
                                    onPrevious = { HapticUtils.playTick(context); homeViewModel.onPreviousPeriod() },
                                    showCustom = false // Hide custom on home screen
                                )
                                
                                BudgetPulseSection(
                                    categoryLimits = categoryLimits,
                                    categorySpentMap = categorySpentMap,
                                    onCategoryClick = { 
                                        HapticUtils.playTick(context)
                                        homeViewModel.selectCategory(it) 
                                    },
                                    isPrivacyMode = privacyMode
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
                        RecurringSection(
                            recurringExpenses = activeRecurringExpenses,
                            expenses = transactions,
                            isPrivacyMode = privacyMode,
                            onViewAllClick = onViewAllRecurringClick,
                            onMarkAsPaid = { homeViewModel.acceptSuggestion(it) },
                            onDismissSuggestion = { homeViewModel.suppressRecurring(it) }
                        )
                    }

                    if (spendingExpenses.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                "Spend Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    DonutChart(
                                        expenses = spendingExpenses, 
                                        modifier = Modifier.weight(1.2f),
                                        selectedCategory = selectedCategory,
                                        onCategoryClick = { homeViewModel.selectCategory(it) },
                                        categoryMap = categoryMap
                                    )
                                    Spacer(modifier = Modifier.width(24.dp))
                                    ChartLegend(
                                        expenses = spendingExpenses, 
                                        modifier = Modifier.weight(1f),
                                        selectedCategory = selectedCategory,
                                        onCategoryClick = { homeViewModel.selectCategory(it) },
                                        categoryMap = categoryMap
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("My Groups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        GroupsList(
                            groups = groups, 
                            expenses = transactions, 
                            onGroupClick = { HapticUtils.playTick(context); onNavigateToGroup(it) }, 
                            onNewGroupClick = { HapticUtils.playTick(context); onCreateGroupClick() },
                            onJoinGroupClick = { HapticUtils.playTick(context); showJoinDialog = true },
                            isPrivacyMode = privacyMode
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                item {
                    val title = if (isSearchActive) {
                        if (searchQuery.isEmpty()) "Recent Transactions" else "Search Results"
                    } else {
                        when (selectedRange) {
                            TimeRange.TODAY -> if (DateUtils.isToday(currentCalendar)) "Today's Transactions" else "Day's Transactions"
                            TimeRange.WEEK -> "Week's Transactions"
                            TimeRange.MONTH -> if (DateUtils.isThisMonth(currentCalendar)) "This Month's Transactions" else "Month's Transactions"
                            TimeRange.YEAR -> "Year's Transactions"
                            TimeRange.ALL -> "Recent Transactions"
                            TimeRange.CUSTOM -> "Filtered Transactions"
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        if (!isSearchActive) TextButton(onClick = onViewAllClick) { Text("View All", color = ElectricBlue, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (filteredExpenses.isEmpty()) {
                    item { EmptyState(isSearching = isSearchActive) }
                } else {
                    items(if (isSearchActive) filteredExpenses else filteredExpenses.take(10), key = { expense: Expense -> expense.id }) { expense ->
                        val details = expense.toTransactionDetails()
                        Box(modifier = Modifier.padding(horizontal = 16.dp).combinedClickable(
                            onClick = { onExpenseClick(expense.id) }, 
                            onLongClick = { 
                                HapticUtils.playHeavyClick(context)
                                showRecurringDialog = expense.id
                            }
                        )) { 
                            TransactionItemCard(
                                transaction = details, 
                                isPrivacyMode = privacyMode,
                                categoryMap = categoryMap
                            ) 
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        if (showCategoryDetail) {
            ModalBottomSheet(
                onDismissRequest = { showCategoryDetail = false; homeViewModel.selectCategory(null) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                selectedCategory?.let { category ->
                    val currentCatObj = categoryMap[category]
                    val style = CategoryStyling.getStyle(category, customCategories = categoryMap, customColorHex = currentCatObj?.colorHex, customIconName = currentCatObj?.iconName)
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
                        CategoryBudgetProgressHero(
                            category = category,
                            spent = categorySpentMap[category] ?: 0.0,
                            limit = categoryLimits[category] ?: 0.0,
                            style = style,
                            isPrivacyMode = privacyMode
                        )

                        if (trendData.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(text = "$category — Last 6 Months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(16.dp))
                            CategoryTrendBarChart(
                                trendData = trendData,
                                categoryColor = style.boldColor,
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showJoinDialog) {
            JoinGroupDialog(
                onJoin = { url ->
                    homeViewModel.joinGroupManual(url, 
                        onComplete = { groupName ->
                            showJoinDialog = false
                            toaster.show("Joined group: $groupName")
                        },
                        onError = { error ->
                            toaster.show(error)
                        }
                    )
                },
                onDismiss = { showJoinDialog = false }
            )
        }

        if (showPermissionDialog) {
            PermissionExplanationDialog(
                onConfirm = {
                    showPermissionDialog = false
                    PermissionUtils.openNotificationSettings(context)
                },
                onDismiss = {
                    showPermissionDialog = false
                    PermissionUtils.recordNudgeDismissed(context)
                }
            )
        }

        if (showBatteryExplanationDialog) {
            BatteryOptimizationExplanationDialog(
                onConfirm = {
                    showBatteryExplanationDialog = false
                    PermissionUtils.requestIgnoreBatteryOptimization(context)
                },
                onDismiss = { showBatteryExplanationDialog = false }
            )
        }

        showRecurringDialog?.let { expenseId ->
            AlertDialog(
                onDismissRequest = { showRecurringDialog = null },
                title = { Text("Recurring Expense", fontWeight = FontWeight.Bold) },
                text = { Text("Would you like to mark this merchant as a recurring expense? Cleave will track it and notify you before the next payment.") },
                confirmButton = {
                    Button(
                        onClick = {
                            homeViewModel.markAsRecurring(expenseId)
                            showRecurringDialog = null
                            toaster.show("Added to recurring expenses")
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Mark as Recurring")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecurringDialog = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (fabExpanded) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { fabExpanded = false })
        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp), contentAlignment = Alignment.BottomEnd) { RefinedFab(isExpanded = fabExpanded, onMainFabClick = { HapticUtils.playTick(context); fabExpanded = !fabExpanded }, onManualEntryClick = { HapticUtils.playTick(context); fabExpanded = false; onAddExpenseClick() }, onScanReceiptClick = { HapticUtils.playTick(context); fabExpanded = false; onScanReceiptClick() }) }
    }
}

// --- REFINED HOME COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    name: String,
    greeting: String,
    onProfileClick: () -> Unit,
    isSearchActive: Boolean,
    onSearchClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    isPrivacyMode: Boolean,
    onPrivacyToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isSearchActive) {
            Column {
                Text(text = "$greeting,", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                // Global Formatting: Title Case for User Name
                Text(text = name.toTitleCase(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrivacyToggle) {
                    Icon(
                        imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Privacy Mode",
                        tint = if (isPrivacyMode) ElectricBlue else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, null, tint = ElectricBlue, modifier = Modifier.size(26.dp))
                }
            }
        } else {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)),
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { IconButton(onClick = onClearSearch) { Icon(Icons.Default.Close, null) } },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }
    }
}

@Composable
fun BalanceSummaryCard(
    currentAmount: Double,
    monthlyTotalSpent: Double,
    selectedRange: TimeRange,
    calendar: java.util.Calendar,
    monthlyBudget: Double?,
    isPrivacyMode: Boolean
) {
    val cardLabel = DateUtils.getPreciseLabel(selectedRange, calendar)
    
    // Rolling number animations
    var triggerRoll by remember { mutableStateOf(false) }
    LaunchedEffect(currentAmount) {
        triggerRoll = true
    }

    val animatedAmount by animateFloatAsState(
        targetValue = if (triggerRoll) currentAmount.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "Main Balance Roll"
    )

    // Calculate Safe to Spend Today
    val remainingBudget = if (monthlyBudget != null) monthlyBudget - monthlyTotalSpent else 0.0
    val daysRemaining = DateUtils.getDaysRemainingInMonth()
    val safeToday = (remainingBudget / daysRemaining).coerceAtLeast(0.0)

    val animatedSafeToday by animateFloatAsState(
        targetValue = if (triggerRoll) safeToday.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "Safe Today Roll"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = ElectricBlue)
    ) {
        Box(
            modifier = Modifier.background(Brush.verticalGradient(colors = listOf(ElectricBlue, LightBlue))).padding(28.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        text = cardLabel,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (monthlyBudget != null) {
                        val safeText = CurrencyMasker.formatSmallAmount(animatedSafeToday.toDouble(), isPrivacyMode)
                        Text(
                            text = "$safeText safe today",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "₹", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    val amountText = if (isPrivacyMode) "••••" else String.format(Locale.getDefault(), "%,.0f", animatedAmount)
                    Text(text = amountText, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                }

                if (monthlyBudget != null) {
                    val actualProgress = (monthlyTotalSpent / monthlyBudget).toFloat().coerceIn(0f, 1.1f)
                    val expectedProgress = DateUtils.getMonthElapsedProgress()
                    val runOutDate = DateUtils.getExpectedRunOutDate(monthlyTotalSpent, monthlyBudget)
                    
                    val isOverspending = actualProgress > (expectedProgress + 0.15f)

                    AnimatedVisibility(visible = isOverspending || actualProgress >= 1.0f, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column {
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(
                                color = Color(0xFFFF5252).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val warningText = when {
                                        actualProgress >= 1.0f -> "BUDGET EXCEEDED — stop spending"
                                        actualProgress > 0.9f -> "OVERSPENDING — runs out soon"
                                        runOutDate != null -> "OVERSPENDING — runs out on $runOutDate"
                                        else -> "OVERSPENDING — pacing too fast"
                                    }
                                    Text(
                                        text = warningText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    LinearProgressIndicator(
                        progress = { actualProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = if (isOverspending || actualProgress >= 1.0f) Color(0xFFFF5252) else Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val usagePercent = (actualProgress * 100).toInt()
                        // Global Formatting: Use formatSmallAmount for the budget limit to get commas
                        val budgetText = CurrencyMasker.formatSmallAmount(monthlyBudget, isPrivacyMode).replace("₹", "")
                        Text("$usagePercent% of ₹$budgetText budget used", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetPulseSection(
    categoryLimits: Map<String, Double>,
    categorySpentMap: Map<String, Double>,
    onCategoryClick: (String) -> Unit,
    isPrivacyMode: Boolean
) {
    if (categoryLimits.isEmpty()) return
    
    val expectedProgress = DateUtils.getMonthElapsedProgress()
    
    // Filter for categories used > 80% OR those overspending relative to pacing
    val atRiskCategories = categoryLimits.keys.filter { category ->
        val spent = categorySpentMap[category] ?: 0.0
        val limit = categoryLimits[category] ?: 0.0
        if (limit <= 0) return@filter false
        
        val actualProgress = (spent / limit).toFloat()
        actualProgress >= 0.8f || actualProgress > (expectedProgress + 0.15f)
    }.sortedByDescending { category ->
        val spent = categorySpentMap[category] ?: 0.0
        val limit = categoryLimits[category] ?: 0.0
        spent / limit
    }

    if (atRiskCategories.isEmpty()) return

    Column {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Budget Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 16.dp)) {
            items(atRiskCategories) { category ->
                val spent = categorySpentMap[category] ?: 0.0
                val limit = categoryLimits[category] ?: 0.0
                val style = CategoryStyling.getStyle(category)
                val actualProgress = (spent / limit).toFloat().coerceIn(0f, 1f)
                val usagePercent = (spent / limit * 100).toInt()
                
                // Smarter status color for health pulses
                val isPacingBad = actualProgress > (expectedProgress + 0.15f)
                val pulseColor = if (actualProgress >= 0.9f || isPacingBad) Color.Red else style.boldColor
                val runOutDate = DateUtils.getExpectedRunOutDate(spent, limit)

                Card(
                    onClick = { onCategoryClick(category) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = pulseColor.copy(alpha = 0.15f)),
                    modifier = Modifier.width(150.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(style.icon, null, tint = pulseColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { actualProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = pulseColor,
                            trackColor = pulseColor.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val statusText = when {
                            usagePercent >= 100 -> "Limit Hit"
                            usagePercent >= 80 -> "$usagePercent% used"
                            runOutDate != null -> "Runs out $runOutDate"
                            isPacingBad -> "Pacing Fast"
                            else -> "$usagePercent% used"
                        }
                        Text(statusText, fontSize = 11.sp, color = pulseColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupsList(
    groups: List<Group>,
    expenses: List<Expense>,
    onGroupClick: (Int) -> Unit,
    onNewGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    isPrivacyMode: Boolean
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp), 
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp)
    ) {
        item {
            DashedAddCard(label = "New Group", icon = Icons.Default.Add, onClick = onNewGroupClick)
        }
        item {
            DashedAddCard(label = "Join Group", icon = Icons.Default.Link, onClick = onJoinGroupClick)
        }
        items(groups, key = { it.groupId }) { group ->
            val groupExpenses = expenses.filter { it.groupId == group.groupId }
            val totalSpent = groupExpenses.sumOf { it.amount }
            
            Card(
                onClick = { onGroupClick(group.groupId) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.width(150.dp).height(180.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = group.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "${group.getMemberList().size} members", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    val amountText = CurrencyMasker.formatSmallAmount(totalSpent, isPrivacyMode)
                    Text(text = amountText, fontWeight = FontWeight.Black, color = ElectricBlue, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

@Composable
private fun DashedAddCard(label: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.5f),
                style = stroke,
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = ElectricBlue, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun RefinedFab(
    isExpanded: Boolean,
    onMainFabClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onScanReceiptClick: () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 45f else 0f, label = "FAB Rotation")
    
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = isExpanded, 
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom), 
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                horizontalAlignment = Alignment.End, 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionFabItem(label = "Manual Entry", icon = Icons.Default.Edit, onClick = onManualEntryClick)
                ActionFabItem(label = "Scan Receipt", icon = Icons.Default.QrCodeScanner, onClick = onScanReceiptClick)
                Spacer(Modifier.height(8.dp))
            }
        }
        FloatingActionButton(
            onClick = onMainFabClick,
            shape = CircleShape,
            containerColor = ElectricBlue,
            contentColor = Color.White,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp).rotate(rotation))
        }
    }
}

@Composable
private fun ActionFabItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier
            .width(200.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        contentColor = ElectricBlue
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ChartLegend(
    expenses: List<Expense>,
    modifier: Modifier = Modifier,
    selectedCategory: String? = null,
    onCategoryClick: (String) -> Unit,
    categoryMap: Map<String, com.context.data.Category> = emptyMap()
) {
    val totalSpend = expenses.sumOf { it.amount }
    val categoryTotals = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }.toList().sortedByDescending { it.second }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categoryTotals.take(6).forEach { (category, amount) ->
            val catObj = categoryMap[category]
            val style = CategoryStyling.getStyle(category, customCategories = categoryMap, customColorHex = catObj?.colorHex, customIconName = catObj?.iconName)
            val isSelected = selectedCategory == category
            val percentage = if (totalSpend > 0) (amount / totalSpend * 100) else 0.0
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onCategoryClick(category) }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(style.boldColor))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = category, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                Text(text = String.format(Locale.getDefault(), "%.1f%%", percentage), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun CategoryBudgetProgressHero(
    category: String,
    spent: Double,
    limit: Double,
    style: CategoryStyle,
    isPrivacyMode: Boolean
) {
    val actualProgress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1.1f) else 0f
    val expectedProgress = DateUtils.getMonthElapsedProgress()
    
    // PACING LOGIC
    val isOverspending = actualProgress > (expectedProgress + 0.15f)

    val remaining = (limit - spent).coerceAtLeast(0.0)
    val daysRemaining = DateUtils.getDaysRemainingInMonth()
    val safeToday = if (daysRemaining > 0) remaining / daysRemaining else 0.0
    val runOutDate = DateUtils.getExpectedRunOutDate(spent, limit)
    
    val statusColor = if (isOverspending || actualProgress >= 1.0f) Color.Red else style.boldColor
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = style.color.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(style.icon, null, tint = style.boldColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text = "$category Budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        if (limit > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    val safeText = CurrencyMasker.formatSmallAmount(safeToday, isPrivacyMode)
                    Text(
                        text = "$safeText/day",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp),
                        fontWeight = FontWeight.Black,
                        color = if (safeToday <= 0 || isOverspending) Color.Red else Color.Black
                    )
                    Text(
                        text = "Safe to spend",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val remainingText = if (isPrivacyMode) "••••" else remaining.toInt().toString()
                    Text(
                        text = when {
                            actualProgress >= 1.0f -> "₹0"
                            runOutDate != null -> runOutDate
                            else -> "₹$remainingText"
                        }, 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Black,
                        color = if (remaining <= 0 || isOverspending) Color.Red else Color.Black
                    )
                    Text(
                        text = if (runOutDate != null && actualProgress < 1.0f) "runs out on" else "remaining",
                        style = MaterialTheme.typography.labelMedium, 
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            LinearProgressIndicator(
                progress = { actualProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = statusColor,
                trackColor = style.color.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val spentText = CurrencyMasker.formatSmallAmount(spent, isPrivacyMode)
                val limitText = if (isPrivacyMode) "••••" else limit.toInt().toString()
                Text(text = "$spentText spent", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = statusColor)
                Text(text = "₹$limitText limit", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            val spentText = CurrencyMasker.formatSmallAmount(spent, isPrivacyMode)
            Text(text = "$spentText spent this month", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = "No limit set for $category", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun EmptyState(isSearching: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(if (isSearching) Icons.Default.SearchOff else Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
        Spacer(Modifier.height(20.dp))
        Text(text = if (isSearching) "No matches found" else "No transactions yet", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

// --- BANNER & DIALOG COMPONENTS ---

@Composable
fun PermissionNudgeBanner(onEnable: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Auto-Tracking", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Automatically log expenses from alerts.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
            TextButton(onClick = onEnable) {
                Text("Enable", fontWeight = FontWeight.Black, color = ElectricBlue)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun BatteryOptimizationNudgeBanner(onFix: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BatteryAlert, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tracking Accuracy", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Exempt Cleave from battery limits.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
            }
            TextButton(onClick = onFix) {
                Text("Fix Now", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun BudgetNudgeBanner(onSetBudgetClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(ElectricBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = ElectricBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Set Monthly Budget", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Plan your spends and save more.", fontSize = 13.sp, color = Color.Gray)
            }
            Button(
                onClick = onSetBudgetClick,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Set Goal", fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun JoinGroupDialog(onJoin: (String) -> Unit, onDismiss: () -> Unit) {
    var inviteUrl by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join a Group", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Paste an invitation link here to join a shared group.", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = inviteUrl,
                    onValueChange = { inviteUrl = it },
                    label = { Text("Invite Link") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { text ->
                                inviteUrl = text
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (inviteUrl.isNotBlank()) onJoin(inviteUrl) },
                enabled = inviteUrl.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PermissionExplanationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Track Expenses", fontWeight = FontWeight.Bold) },
        text = {
            Text("Cleave needs permission to read payment notifications to automatically track your expenses. This data is processed strictly locally and never leaves your device.")
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp)) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun BatteryOptimizationExplanationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Background Tracking", fontWeight = FontWeight.Bold) },
        text = {
            Text("To ensure auto-tracking works reliably, Cleave needs to run in the background. Please allow it to 'Ignore Battery Optimizations' in the next screen.")
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp)) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
