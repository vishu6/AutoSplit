package com.context.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.components.ModernSearchBar
import com.context.components.ModernTimeRangeFilter
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ElectricBlue
import com.context.utils.HapticUtils
import com.context.utils.SecurityUtils
import com.context.utils.SortOrder
import com.context.utils.TimeRange
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllTransactionsScreen(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    onExpenseClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val filteredExpenses by homeViewModel.filteredExpenses.collectAsState()
    val selectedRange by homeViewModel.selectedTimeRange.collectAsState()
    val customRange by homeViewModel.customDateRange.collectAsState()
    val currentCalendar by homeViewModel.currentCalendar.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val isSearchActive by homeViewModel.isSearchActive.collectAsState()
    
    val allCategories by homeViewModel.allCategoriesList.collectAsState()
    val selectedCategories by homeViewModel.selectedCategories.collectAsState()
    val sortOrder by homeViewModel.selectedSortOrder.collectAsState()
    val categoryMap by homeViewModel.categoryMap.collectAsState()
    
    val privacyMode by remember { SecurityUtils.getPrivacyModeFlow(context) }.collectAsState(initial = SecurityUtils.isPrivacyModeEnabled(context))

    var showFilterSheet by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState()

    BackHandler(enabled = isSearchActive || showFilterSheet) {
        if (showFilterSheet) showFilterSheet = false
        else if (isSearchActive) homeViewModel.setSearchActive(false)
    }

    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        homeViewModel.onCustomDateRangeSelected(start, end)
                        showDateRangePicker = false
                    }
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(400.dp)
            )
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter & Sort", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { homeViewModel.resetAllFilters() }) {
                        Text("Reset All")
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("Sort Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SortOrder.entries.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { homeViewModel.onSortOrderSelected(order) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == order,
                            onClick = { homeViewModel.onSortOrderSelected(order) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(order.label)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("Date Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showDateRangePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.FilterList, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedRange == TimeRange.CUSTOM && customRange != null) {
                        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        "${fmt.format(Date(customRange!!.first))} - ${fmt.format(Date(customRange!!.second))}"
                    } else "Select Custom Range")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!isSearchActive) {
                        Text("All Transactions", fontWeight = FontWeight.Bold)
                    } else {
                        ModernSearchBar(
                            query = searchQuery,
                            onQueryChange = { homeViewModel.onSearchQueryChanged(it) },
                            onClearSearch = { homeViewModel.setSearchActive(false) },
                            placeholder = "Search in merchant..."
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtils.playTick(context)
                        if (isSearchActive) homeViewModel.setSearchActive(false) else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = {
                            HapticUtils.playTick(context)
                            homeViewModel.setSearchActive(true)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { 
                            HapticUtils.playTick(context)
                            showFilterSheet = true 
                        }) {
                            Icon(Icons.Default.Sort, contentDescription = "Filter & Sort")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val customLabel = if (selectedRange == TimeRange.CUSTOM && customRange != null) {
                val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
                "${fmt.format(Date(customRange!!.first))} - ${fmt.format(Date(customRange!!.second))}"
            } else null

            ModernTimeRangeFilter(
                selectedRange = selectedRange,
                onRangeSelected = { range ->
                    HapticUtils.playTick(context)
                    if (range == TimeRange.CUSTOM) showDateRangePicker = true
                    else homeViewModel.onTimeRangeSelected(range)
                },
                calendar = currentCalendar,
                onNext = { homeViewModel.onNextPeriod() },
                onPrevious = { homeViewModel.onPreviousPeriod() },
                customRangeLabel = customLabel,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategories.isEmpty(),
                        onClick = { homeViewModel.clearCategoryFilters() },
                        label = { Text("All") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(allCategories) { category ->
                    val isSelected = selectedCategories.contains(category.name)
                    val style = CategoryStyling.getStyle(category.name, customColorHex = category.colorHex, customIconName = category.iconName)
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { homeViewModel.toggleCategorySelection(category.name) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                style.icon, 
                                null, 
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else style.boldColor
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No transactions found" else "No matches for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(filteredExpenses) { expense ->
                        val details = expense.toTransactionDetails()
                        Column(
                            modifier = Modifier.combinedClickable(
                                onClick = { onExpenseClick(expense.id) },
                                onLongClick = {
                                    HapticUtils.playHeavyClick(context)
                                    onExpenseClick(expense.id)
                                }
                            )
                        ) {
                            TransactionItemCard(
                                transaction = details,
                                isPrivacyMode = privacyMode,
                                categoryMap = categoryMap
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}
