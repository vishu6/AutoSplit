package com.context.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.context.utils.ReceiptItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    startWithScanner: Boolean = false,
    onBack: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val db = remember { ExpenseDatabase.getDatabase(context) }
    val scrollState = rememberScrollState()

    // -- STATE --
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Date State
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)

    // Category State
    var selectedCategory by remember { mutableStateOf("Other") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories = remember { CategoryUtils.categories }

    // Group Selection State
    val groups by db.expenseDao().getAllGroups().collectAsState(initial = emptyList())
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var isGroupDropdownExpandedForGroups by remember { mutableStateOf(false) }

    // Paid By State
    var paidBy by remember { mutableStateOf("You") }
    var isPaidByDropdownExpanded by remember { mutableStateOf(false) }

    // Receipt Scanner State
    var showScanner by remember { mutableStateOf(startWithScanner) }
    var showAssignment by remember { mutableStateOf(false) }
    var detectedItems by remember { mutableStateOf<List<ReceiptItem>>(emptyList()) }

    // Split Calculation
    val amountDouble = amount.toDoubleOrNull() ?: 0.0

    if (showScanner) {
        ReceiptScannerScreen(
            onItemsParsed = { items ->
                detectedItems = items
                showScanner = false
                showAssignment = true
            },
            onBack = { 
                if (startWithScanner && amount.isEmpty()) {
                    onBack()
                } else {
                    showScanner = false 
                }
            }
        )
        return
    }

    if (showAssignment) {
        ReceiptAssignmentScreen(
            items = detectedItems,
            members = selectedGroup?.getMemberList() ?: listOf("You"),
            onComplete = { total ->
                amount = String.format("%.2f", total)
                showAssignment = false
            },
            onBack = { showAssignment = false }
        )
        return
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
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .imePadding() // Ensures keyboard doesn't hide content
                .verticalScroll(scrollState), // Added vertical scroll
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text("Amount") },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₹", style = MaterialTheme.typography.titleLarge) },
                singleLine = true
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
                placeholder = { Text("e.g. Dinner with friends") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DATE SELECTOR
            OutlinedTextField(
                value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedTimestamp)),
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false, // Use clickable on modifier instead
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CATEGORY SELECTOR
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

            // GROUP SELECTOR
            ExposedDropdownMenuBox(
                expanded = isGroupDropdownExpandedForGroups,
                onExpandedChange = { isGroupDropdownExpandedForGroups = it }
            ) {
                OutlinedTextField(
                    value = selectedGroup?.name ?: "Personal Expense",
                    onValueChange = {},
                    label = { Text("Group") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupDropdownExpandedForGroups) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isGroupDropdownExpandedForGroups,
                    onDismissRequest = { isGroupDropdownExpandedForGroups = false }
                ) {
                    DropdownMenuItem(text = { Text("Personal") }, onClick = { selectedGroup = null; isGroupDropdownExpandedForGroups = false })
                    groups.forEach { group ->
                        DropdownMenuItem(text = { Text(group.name) }, onClick = { 
                            selectedGroup = group
                            isGroupDropdownExpandedForGroups = false
                            if (paidBy != "You" && !group.getMemberList().contains(paidBy)) {
                                paidBy = "You"
                            }
                        })
                    }
                }
            }

            // PAID BY SELECTOR
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

            // DYNAMIC SPLIT PREVIEW
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

            Spacer(modifier = Modifier.height(32.dp))

            // SAVE BUTTON
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null && amountVal > 0) {
                        scope.launch {
                            val newExpense = Expense(
                                merchant = description.ifBlank { selectedCategory },
                                amount = amountVal,
                                timestamp = selectedTimestamp, // Use selected date
                                category = selectedCategory,
                                groupId = selectedGroup?.groupId,
                                paidBy = if (selectedGroup == null) "You" else paidBy,
                                isAuto = false
                            )

                            db.expenseDao().insert(newExpense)

                            selectedGroup?.groupId?.let {
                                db.expenseDao().recalculateGroupTotal(it)
                            }

                            // Haptic feedback for manual save
                            HapticUtils.playDoubleTick(context)
                            
                            // Check budget thresholds
                            BudgetUtils.checkAndNotifyBudget(context)

                            toaster.show("Saved!")
                            onExpenseAdded()
                        }
                    } else {
                        toaster.show("Enter a valid amount")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Expense", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun SplitPreviewCard(
    members: List<String>,
    totalAmount: Double,
    payer: String = "You"
) {
    val perPerson = if (members.isNotEmpty()) totalAmount / members.size else 0.0
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Split Breakdown",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            members.forEach { member ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = member.take(1).uppercase(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        val displayText = when {
                            member == payer && payer == "You" -> "You paid"
                            member == payer -> "$member paid"
                            member == "You" -> "You owe $payer"
                            else -> "$member owes $payer"
                        }
                        
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "₹${String.format("%.2f", perPerson)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
