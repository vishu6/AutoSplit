package com.context.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.context.data.Category
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ContextTheme
import com.context.ui.theme.ElectricBlue
import com.context.ui.theme.FintechRed
import com.context.utils.BudgetUtils
import com.context.utils.HapticUtils
import com.context.utils.LocalToaster
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onBack: () -> Unit,
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val scrollState = rememberScrollState()

    // Dynamic Categories from DB
    val dbCategories by categoryViewModel.allCategories.collectAsState(initial = emptyList())
    val categoryMap by remember(dbCategories) { derivedStateOf { dbCategories.associateBy { it.name } } }
    
    var monthlyBudgetStr by remember { mutableStateOf(BudgetUtils.getMonthlyBudget(context).let { if (it > 0) it.toInt().toString() else "" }) }
    
    // We use a local state map to track edits before saving
    val categoryLimits = remember { mutableStateMapOf<String, String>() }
    
    // Sync local state when categories or existing limits are loaded
    LaunchedEffect(dbCategories) {
        val initialLimits = BudgetUtils.getCategoryLimits(context)
        dbCategories.forEach { cat ->
            if (!categoryLimits.containsKey(cat.name)) {
                categoryLimits[cat.name] = initialLimits[cat.name]?.let { if (it > 0) it.toInt().toString() else "" } ?: ""
            }
        }
    }

    var lastMonthTotals by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var currentMonthTotals by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    LaunchedEffect(Unit) {
        lastMonthTotals = BudgetUtils.getCategoryTotalsForPeriod(context, isCurrentMonth = false)
        currentMonthTotals = BudgetUtils.getCategoryTotalsForPeriod(context, isCurrentMonth = true)
    }

    val totalMonthlyBudget = monthlyBudgetStr.toDoubleOrNull() ?: 0.0
    val totalAllocated = categoryLimits.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val remainingToAllocate = totalMonthlyBudget - totalAllocated
    val isOverAllocated = totalMonthlyBudget > 0 && remainingToAllocate < 0

    // Smart Sorting: Custom logic to prioritize active categories
    val sortedCategories = remember(dbCategories, currentMonthTotals, categoryLimits.size) {
        dbCategories.sortedByDescending { (currentMonthTotals[it.name] ?: 0.0) + (if (categoryLimits[it.name]?.isNotEmpty() == true) 1000000.0 else 0.0) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Planner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 12.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (remainingToAllocate >= 0) "Left to Plan" else "Over Budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverAllocated) FintechRed else Color.Gray
                        )
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.0f", abs(remainingToAllocate))}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverAllocated) FintechRed else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val budgetVal = monthlyBudgetStr.toDoubleOrNull() ?: 0.0
                            BudgetUtils.setMonthlyBudget(context, budgetVal)
                            
                            val limitsMap = categoryLimits.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                            BudgetUtils.setCategoryLimits(context, limitsMap)
                            
                            HapticUtils.playDoubleTick(context)
                            toaster.show("Budget saved successfully!")
                            onBack()
                        },
                        modifier = Modifier.height(56.dp).widthIn(min = 120.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = totalMonthlyBudget > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverAllocated) FintechRed else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save Plan", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Monthly Goal Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Monthly Spend Goal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val inputStyle = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    OutlinedTextField(
                        value = monthlyBudgetStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) monthlyBudgetStr = it },
                        textStyle = inputStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "₹", style = inputStyle)
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        },
                        placeholder = { 
                            Text(
                                text = "0", 
                                color = Color.LightGray,
                                style = inputStyle
                            ) 
                        },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Allocation Donut Chart
            if (totalAllocated > 0) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    AllocationDonutChart(
                        limits = categoryLimits.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
                        categoryMap = categoryMap
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PLANNED", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${totalAllocated.toInt()}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Category Targets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (lastMonthTotals.values.any { it > 0 }) {
                    IconButton(
                        onClick = {
                            HapticUtils.playDoubleTick(context)
                            lastMonthTotals.forEach { (cat, total) ->
                                if (total > 0) categoryLimits[cat] = total.toInt().toString()
                            }
                            val totalLastMonth = lastMonthTotals.values.sum()
                            monthlyBudgetStr = totalLastMonth.toInt().toString()
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Text(
                "Compare with your real-world spending habits.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Category Items
            sortedCategories.forEach { category ->
                CategoryLimitCard(
                    categoryName = category.name,
                    categoryObj = category,
                    limitStr = categoryLimits[category.name] ?: "",
                    onLimitChange = { categoryLimits[category.name] = it },
                    totalMonthlyBudget = totalMonthlyBudget,
                    lastMonthSpend = lastMonthTotals[category.name] ?: 0.0,
                    currentMonthSpend = currentMonthTotals[category.name] ?: 0.0
                )
            }
            
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun AllocationDonutChart(
    limits: Map<String, Double>,
    categoryMap: Map<String, Category>
) {
    val total = limits.values.sum()
    if (total <= 0) return

    Canvas(modifier = Modifier.size(140.dp)) {
        var startAngle = -90f
        val strokeWidth = 16.dp.toPx()
        
        limits.forEach { (categoryName, amount) ->
            if (amount > 0) {
                val sweepAngle = (amount / total).toFloat() * 360f
                val catObj = categoryMap[categoryName]
                val color = CategoryStyling.getStyle(
                    categoryName = categoryName,
                    customColorHex = catObj?.colorHex,
                    customIconName = catObj?.iconName
                ).boldColor
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun CategoryLimitCard(
    categoryName: String,
    categoryObj: Category?,
    limitStr: String,
    onLimitChange: (String) -> Unit,
    totalMonthlyBudget: Double,
    lastMonthSpend: Double,
    currentMonthSpend: Double
) {
    val context = LocalContext.current
    val style = remember(categoryName, categoryObj) { 
        CategoryStyling.getStyle(
            categoryName = categoryName,
            customColorHex = categoryObj?.colorHex,
            customIconName = categoryObj?.iconName
        ) 
    }
    val limitValue = limitStr.toDoubleOrNull() ?: 0.0
    val isUnderLimit = limitValue > 0 && limitValue < currentMonthSpend

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(if (isUnderLimit) Modifier.border(1.dp, FintechRed, RoundedCornerShape(24.dp)) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (limitValue > 0) style.color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(style.color.copy(alpha = 0.2f)), 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(style.icon, null, tint = style.boldColor, modifier = Modifier.size(20.dp))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = categoryName, 
                    fontWeight = FontWeight.ExtraBold, 
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { 
                            HapticUtils.playTick(context)
                            val current = limitStr.toDoubleOrNull() ?: 0.0
                            onLimitChange(maxOf(0.0, current - 500).toInt().toString())
                        },
                        modifier = Modifier.size(28.dp)
                    ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp), tint = style.boldColor) }
                    
                    BasicTextField(
                        value = if (limitValue > 0) "₹${limitValue.toInt()}" else "₹0",
                        onValueChange = { 
                            val digits = it.filter { c -> c.isDigit() }
                            onLimitChange(digits)
                        },
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.widthIn(min = 50.dp, max = 85.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = { 
                            HapticUtils.playTick(context)
                            val current = limitStr.toDoubleOrNull() ?: 0.0
                            onLimitChange((current + 500).toInt().toString())
                        },
                        modifier = Modifier.size(28.dp)
                    ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = style.boldColor) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp)
            ) {
                if (currentMonthSpend > 0) {
                    Text(
                        text = "Spent: ₹${currentMonthSpend.toInt()}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (isUnderLimit) FintechRed else style.boldColor,
                        maxLines = 1
                    )
                }
                if (currentMonthSpend > 0 && lastMonthSpend > 0) {
                    Text(" • ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                if (lastMonthSpend > 0) {
                    Text(
                        text = "Last: ₹${lastMonthSpend.toInt()}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }

            if (isUnderLimit) {
                Text(
                    "⚠️ Limit is lower than current spend",
                    color = FintechRed,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp, start = 52.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
