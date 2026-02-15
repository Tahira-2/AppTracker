package com.alertsystem.apptracker.domain.repository

import com.alertsystem.apptracker.data.local.entity.AppSettingsEntity
import com.alertsystem.apptracker.data.local.entity.DailyUsageEntity
import com.alertsystem.apptracker.data.local.entity.UsageSessionEntity
import com.alertsystem.apptracker.data.model.AddictionMetrics
import com.alertsystem.apptracker.data.model.AppUsageInfo
import kotlinx.coroutines.flow.Flow

interface UsageRepository {

    // App Settings
    fun getAllAppSettings(): Flow<List<AppSettingsEntity>>
    suspend fun getAppSettings(packageName: String): AppSettingsEntity?
    suspend fun saveAppSettings(settings: AppSettingsEntity)
    suspend fun updateTimeLimit(packageName: String, limitMinutes: Int)
    suspend fun updateExclusion(packageName: String, isExcluded: Boolean, confirmCount: Int)
    suspend fun updateLastNotificationTime(packageName: String, time: Long)
    suspend fun updateNotificationEnabled(packageName: String, enabled: Boolean)
    suspend fun updateAddictive(packageName: String, isAddictive: Boolean)
    suspend fun updateAddedTime(packageName: String, minutes: Int, increment: Int)
    suspend fun updateUnsubscribeConfirmCount(packageName: String, count: Int)
    suspend fun resetAddedTimeForApp(packageName: String)
    fun getAddictiveApps(): Flow<List<AppSettingsEntity>>

    // Daily Usage
    fun getTodayUsage(): Flow<List<DailyUsageEntity>>
    fun getTotalUsageToday(): Flow<Long?>
    suspend fun getTodayUsageForPackage(packageName: String): Long
    suspend fun getUsageForDate(date: String): List<DailyUsageEntity>
    suspend fun getUsageBetweenDates(startDate: String, endDate: String): List<DailyUsageEntity>
    suspend fun recordUsageTime(packageName: String, appName: String, seconds: Long, isLateNight: Boolean)
    suspend fun incrementOpenCount(packageName: String, appName: String)
    suspend fun incrementShortSessionCount(packageName: String)

    // Usage Sessions
    suspend fun saveSession(session: UsageSessionEntity)
    fun getRecentSessions(limit: Int): Flow<List<UsageSessionEntity>>
    suspend fun getSessionsForDate(date: String, packageName: String): List<UsageSessionEntity>

    // Addiction Detection
    suspend fun getAddictionMetrics(packageName: String): AddictionMetrics

    // Installed Apps
    suspend fun getInstalledApps(): List<AppUsageInfo>
    suspend fun syncInstalledApps()

    // Cleanup
    suspend fun cleanupOldData(daysToKeep: Int)
}
