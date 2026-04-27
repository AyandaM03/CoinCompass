package com.example.coincompass.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val budgetAmount: Double = 0.0 // Maximum goal/budget for this category
)
