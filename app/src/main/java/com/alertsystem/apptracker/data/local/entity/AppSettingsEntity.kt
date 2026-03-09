package com.alertsystem.apptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val timeLimitMinutes: Int = 15,
    val isExcluded: Boolean = false,
    val exclusionConfirmCount: Int = 0,
    val lastNotificationTime: Long = 0,
    val notificationIntervalMinutes: Int = 5,
    // New fields for notification selection
    val isNotificationEnabled: Boolean = false, // Default: no notifications
    val isAddictive: Boolean = false, // Marked as addictive (auto or manual)
    val addictiveManualOverride: Boolean = false, // Deprecated: kept for DB schema compatibility
    val addictiveOverrideUntil: Long = 0, // Timestamp until which manual unmark override is active (0 = no override)
    val addedTimeMinutes: Int = 0, // Time added via "Add Time" option
    val currentAddTimeIncrement: Int = 5, // Current increment for "Add Time" (5, 10, 15...)
    val unsubscribeConfirmCount: Int = 0, // Confirmations needed to unsubscribe addictive app
    val isWork: Boolean = false // Work apps won't be auto-marked as addictive
)
