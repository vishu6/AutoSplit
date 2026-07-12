package com.context.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.Category
import com.context.data.ExpenseDatabase
import com.context.utils.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ExpenseDatabase.getDatabase(application)
    private val dao = db.expenseDao()

    val allCategories: Flow<List<Category>> = dao.getAllCategories()

    init {
        seedDefaultCategories()
    }

    private fun seedDefaultCategories() {
        viewModelScope.launch {
            // Check if seeding is needed (simplified check)
            // In a real app, you might use a DataStore flag, but checking if empty is safe here.
            dao.getAllCategories().collect { list ->
                if (list.isEmpty()) {
                    val defaults = ExpenseCategory.values().map { 
                        Category(
                            name = it.label,
                            iconName = it.name, // Store enum name for lookup
                            colorHex = "#2962FF", // Default blue, Styling.kt handles system colors
                            isSystem = true
                        )
                    }
                    defaults.forEach { dao.insertCategory(it) }
                }
            }
        }
    }

    fun addCustomCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            dao.insertCategory(
                Category(
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    isSystem = false
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        if (!category.isSystem) {
            viewModelScope.launch {
                dao.deleteCategory(category)
            }
        }
    }
}
