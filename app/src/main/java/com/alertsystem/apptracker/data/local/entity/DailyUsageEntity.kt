package com.alertsystem.apptracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_usage",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val date: String, // Format: yyyy-MM-dd
    val totalUsageSeconds: Long = 0,
    val openCount: Int = 0,
    val shortSessionCount: Int = 0, // Sessions < 2 minutes (compulsive checking)
    val lateNightUsageSeconds: Long = 0 // Usage between 11 PM - 6 AM
)
