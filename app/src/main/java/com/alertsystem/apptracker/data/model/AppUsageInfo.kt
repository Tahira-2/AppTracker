package com.alertsystem.apptracker.data.model

import android.graphics.drawable.Drawable

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val todayUsageSeconds: Long = 0,
    val timeLimitMinutes: Int = 15,
    val isExcluded: Boolean = false,
    val isAddictive: Boolean = false,
    val openCount: Int = 0
)

data class UsageSession(
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long
)

data class DailyUsageSummary(
    val date: String,
    val totalUsageSeconds: Long,
    val appUsageList: List<AppUsageInfo>
)

data class AddictionMetrics(
    val packageName: String,
    val avgDailyUsageSeconds: Double,
    val avgOpenCount: Double,
    val avgShortSessions: Double,
    val avgLateNightUsageSeconds: Double,
    val isAddictive: Boolean,
    val addictionScore: Int // 0-4 based on criteria met
)
