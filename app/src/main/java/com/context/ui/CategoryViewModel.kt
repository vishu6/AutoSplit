package com.context.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.Category
import com.context.data.ExpenseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ExpenseDatabase.getDatabase(application)
    private val dao = db.expenseDao()

    val allCategories: Flow<List<Category>> = dao.getAllCategories()

    // Seeding logic removed from here as it now happens in HomeViewModel for earlier availability

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
