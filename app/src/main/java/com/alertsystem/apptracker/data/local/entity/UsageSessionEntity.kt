package com.alertsystem.apptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_sessions")
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val startTime: Long, // Epoch milliseconds
    val endTime: Long, // Epoch milliseconds
    val durationSeconds: Long,
    val date: String // Format: yyyy-MM-dd
)
