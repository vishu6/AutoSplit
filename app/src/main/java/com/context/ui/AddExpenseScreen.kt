package com.context.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.context.utils.CategoryUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ExpenseDatabase.getDatabase(context) }

    // -- STATE --
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Category State
    var selectedCategory by remember { mutableStateOf("Other") } // Default category
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories = remember { CategoryUtils.categories }

    // Group Selection State
    val groups by db.expenseDao().getAllGroups().collectAsState(initial = emptyList())
    var selectedGroup by remember { mutableStateOf<Group?>(null) } // null = Personal
    var isGroupDropdownExpandedForGroups by remember { mutableStateOf(false) }

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
                .fillMaxSize(),
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
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("e.g. Dinner with friends") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CATEGORY SELECTOR
            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {}, // Read-only
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
                    onValueChange = {}, // Read-only
                    label = { Text("Group") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupDropdownExpandedForGroups) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isGroupDropdownExpandedForGroups,
                    onDismissRequest = { isGroupDropdownExpandedForGroups = false }
                ) {
                    DropdownMenuItem(text = { Text("Personal") }, onClick = { selectedGroup = null; isGroupDropdownExpandedForGroups = false })
                    groups.forEach { group ->
                        DropdownMenuItem(text = { Text(group.name) }, onClick = { selectedGroup = group; isGroupDropdownExpandedForGroups = false })
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Push button to bottom

            // 4. SAVE BUTTON
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null && amountVal > 0) {
                        scope.launch {
                            val newExpense = Expense(
                                merchant = description.ifBlank { selectedCategory },
                                amount = amountVal,
                                timestamp = System.currentTimeMillis(),
                                category = selectedCategory,
                                groupId = selectedGroup?.groupId,
                                paidBy = "You",
                                isAuto = false
                            )

                            db.expenseDao().insert(newExpense)

                            selectedGroup?.groupId?.let {
                                db.expenseDao().recalculateGroupTotal(it)
                            }

                            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                            onExpenseAdded()
                        }
                    } else {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
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