package com.example.coincompass.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String, // YYYY-MM
    val minGoal: Double,
    val maxGoal: Double
)
