package com.example.coincompass.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_points")
data class RewardPoints(
    @PrimaryKey val id: Int = 1, // Only one row for user stats
    val points: Int = 0,
    val level: Int = 1,
    val totalGamesPlayed: Int = 0,
    val badgesEarned: String = "" // Comma-separated badge names
)
