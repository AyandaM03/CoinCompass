package com.example.coincompass.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val description: String,
    val categoryName: String,
    val amount: Double,
    val type: String = "Expense", // "Income" or "Expense"
    val photoPath: String? = null
)
