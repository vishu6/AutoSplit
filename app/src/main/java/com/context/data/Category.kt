package com.context.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,
    val iconName: String,
    val colorHex: String,
    val isSystem: Boolean = false
)
