package com.context.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.context.data.Category
import com.context.ui.theme.CategoryStyling
import com.context.utils.HapticUtils
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    HapticUtils.playTick(context)
                    showAddDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "System Categories",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(categories.filter { it.isSystem }, key = { it.name }) { category ->
                CategoryItem(category = category, onDelete = null)
            }

            val customCats = categories.filter { !it.isSystem }
            if (customCats.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Custom Categories",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(customCats, key = { it.name }) { category ->
                    CategoryItem(
                        category = category,
                        onDelete = { 
                            HapticUtils.playThud(context)
                            viewModel.deleteCategory(it) 
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, icon, color ->
                viewModel.addCustomCategory(name, icon, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onDelete: ((Category) -> Unit)?
) {
    // FIXED: Passing iconName and colorHex from the category object to ensure custom icons show correctly
    val style = CategoryStyling.getStyle(
        categoryName = category.name,
        customColorHex = category.colorHex,
        customIconName = category.iconName
    )
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = style.color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(style.icon, null, tint = style.boldColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (onDelete != null) {
                IconButton(onClick = { onDelete(category) }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Gray.copy(alpha = 0.6f))
                }
            } else {
                Icon(Icons.Default.Lock, "System", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Favorite") }
    var selectedColor by remember { mutableStateOf(Color(0xFF2962FF)) }

    val icons = listOf(
        "Favorite" to Icons.Default.Favorite,
        "Pets" to Icons.Default.Pets,
        "Brush" to Icons.Default.Brush,
        "Sports" to Icons.Default.SportsBasketball,
        "Music" to Icons.Default.MusicNote,
        "Home" to Icons.Default.Home,
        "Star" to Icons.Default.Star,
        "Coffee" to Icons.Default.Coffee,
        "Laptop" to Icons.Default.Laptop,
        "Camera" to Icons.Default.PhotoCamera
    )

    val colors = listOf(
        Color(0xFF2962FF), Color(0xFFE91E63), Color(0xFF9C27B0),
        Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF795548),
        Color(0xFF607D8B), Color(0xFF00BCD4)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(Modifier.height(20.dp))
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(100.dp)
                ) {
                    items(icons) { (iconName, icon) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (selectedIconName == iconName) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedIconName = iconName }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = if (selectedIconName == iconName) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Theme Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(color, CircleShape)
                                .clickable { selectedColor = color }
                                .padding(4.dp)
                        ) {
                            if (selectedColor == color) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedIconName, String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
