package com.context.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.data.Group
import com.context.ui.theme.CategoryStyling
import com.context.utils.BudgetUtils
import com.context.utils.CategoryEngine
import com.context.utils.CategoryUtils
import com.context.utils.ExpenseCategory
import com.context.utils.HapticUtils
import com.context.utils.LocalToaster
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    expenseId: Int,
    onBack: () -> Unit,
    onExpenseUpdated: () -> Unit
) {
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val db = remember { ExpenseDatabase.getDatabase(context) }
    val scrollState = rememberScrollState()

    // State
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var existingExpense by remember { mutableStateOf<Expense?>(null) }
    
    // Date State
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)

    // Group Selector State
    val groups by db.expenseDao().getAllGroups().collectAsState(initial = emptyList())
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var isGroupDropdownExpanded by remember { mutableStateOf(false) }

    // Category Selector State
    var selectedCategory by remember { mutableStateOf("") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories = remember { CategoryUtils.categories }

    // Paid By State
    var paidBy by remember { mutableStateOf("You") }
    var isPaidByDropdownExpanded by remember { mutableStateOf(false) }

    // Split Calculation
    val amountDouble = amount.toDoubleOrNull() ?: 0.0

    // LOAD DATA
    LaunchedEffect(expenseId) {
        val exp = db.expenseDao().getExpenseById(expenseId)
        if (exp != null) {
            existingExpense = exp
            amount = exp.amount.toString().replace(".0", "") // Clean format
            description = exp.merchant
            selectedCategory = exp.category
            selectedTimestamp = exp.timestamp
            paidBy = exp.paidBy
        }
    }

    // Sync selected group once groups are loaded
    LaunchedEffect(groups, existingExpense) {
        if (existingExpense?.groupId != null && groups.isNotEmpty()) {
            selectedGroup = groups.find { it.groupId == existingExpense?.groupId }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTimestamp = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            existingExpense?.let { expenseToDelete ->
                                val groupId = expenseToDelete.groupId
                                db.expenseDao().delete(expenseToDelete)
                                groupId?.let { db.expenseDao().recalculateGroupTotal(it) }
                                
                                // Haptic feedback for deletion
                                HapticUtils.playHeavyClick(context)
                            }
                            toaster.show("Deleted")
                            onExpenseUpdated()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        // Use a Column for the whole screen
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding() // Pushes everything up when keyboard opens
        ) {
            // Scrollable Content area takes up available space
            Column(
                modifier = Modifier
                    .weight(1f) // Fills space above the button
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                    label = { Text("Amount") },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹", style = MaterialTheme.typography.titleLarge) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        val predicted = CategoryEngine.predictCategory(it)
                        if (predicted != ExpenseCategory.OTHER) {
                            selectedCategory = predicted.label
                        }
                    },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedTimestamp)),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        leadingIcon = {
                            val style = CategoryStyling.getStyle(selectedCategory)
                            Icon(style.icon, contentDescription = null, tint = style.color)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categories.forEach { category ->
                            val style = CategoryStyling.getStyle(category)
                            DropdownMenuItem(
                                text = { Text(category) },
                                leadingIcon = { Icon(style.icon, contentDescription = null, tint = style.color) },
                                onClick = { 
                                    selectedCategory = category 
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = isGroupDropdownExpanded,
                    onExpandedChange = { isGroupDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "Personal Expense",
                        onValueChange = {},
                        label = { Text("Group") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isGroupDropdownExpanded,
                        onDismissRequest = { isGroupDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Personal") }, onClick = { selectedGroup = null; isGroupDropdownExpanded = false })
                        groups.forEach { group ->
                            DropdownMenuItem(text = { Text(group.name) }, onClick = { 
                                selectedGroup = group
                                isGroupDropdownExpanded = false 
                                if (paidBy != "You" && !group.getMemberList().contains(paidBy)) {
                                    paidBy = "You"
                                }
                            })
                        }
                    }
                }

                if (selectedGroup != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExposedDropdownMenuBox(
                        expanded = isPaidByDropdownExpanded,
                        onExpandedChange = { isPaidByDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = paidBy,
                            onValueChange = {},
                            label = { Text("Paid By") },
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaidByDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isPaidByDropdownExpanded,
                            onDismissRequest = { isPaidByDropdownExpanded = false }
                        ) {
                            val members = selectedGroup?.getMemberList() ?: listOf("You")
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member) },
                                    onClick = {
                                        paidBy = member
                                        isPaidByDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedGroup != null && amountDouble > 0,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        SplitPreviewCard(
                            members = selectedGroup?.getMemberList() ?: emptyList(),
                            totalAmount = amountDouble,
                            payer = paidBy
                        )
                    }
                }
            }

            // Fixed Button at bottom (Outside the scrollable Column)
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            if (amtVal > 0) {
                                existingExpense?.let { oldExp ->
                                    val oldGroupId = oldExp.groupId
                                    val updatedExp = oldExp.copy(
                                        amount = amtVal,
                                        merchant = description,
                                        timestamp = selectedTimestamp,
                                        groupId = selectedGroup?.groupId,
                                        category = selectedCategory,
                                        paidBy = if (selectedGroup == null) "You" else paidBy
                                    )
                                    db.expenseDao().update(updatedExp)
                                    updatedExp.groupId?.let { db.expenseDao().recalculateGroupTotal(it) }
                                    if (oldGroupId != updatedExp.groupId) {
                                        oldGroupId?.let { db.expenseDao().recalculateGroupTotal(it) }
                                    }
                                    
                                    // Haptic feedback for update success
                                    HapticUtils.playDoubleTick(context)
                                    
                                    // Check budget thresholds
                                    BudgetUtils.checkAndNotifyBudget(context)

                                    onExpenseUpdated()
                                }
                            } else {
                                toaster.show("Enter a valid amount")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Expense", fontSize = 18.sp)
                }
            }
        }
    }
}
