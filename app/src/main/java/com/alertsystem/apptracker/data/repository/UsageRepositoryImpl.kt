package com.alertsystem.apptracker.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.alertsystem.apptracker.data.local.dao.AppSettingsDao
import com.alertsystem.apptracker.data.local.dao.DailyUsageDao
import com.alertsystem.apptracker.data.local.dao.UsageSessionDao
import com.alertsystem.apptracker.data.local.entity.AppSettingsEntity
import com.alertsystem.apptracker.data.local.entity.DailyUsageEntity
import com.alertsystem.apptracker.data.local.entity.UsageSessionEntity
import com.alertsystem.apptracker.data.model.AddictionMetrics
import com.alertsystem.apptracker.data.model.AppUsageInfo
import com.alertsystem.apptracker.domain.repository.UsageRepository
import com.alertsystem.apptracker.util.AddictionDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsDao: AppSettingsDao,
    private val dailyUsageDao: DailyUsageDao,
    private val usageSessionDao: UsageSessionDao,
    private val addictionDetector: AddictionDetector
) : UsageRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun getAllAppSettings(): Flow<List<AppSettingsEntity>> {
        return appSettingsDao.getAllSettings()
    }

    override suspend fun getAppSettings(packageName: String): AppSettingsEntity? {
        return appSettingsDao.getSettingsByPackage(packageName)
    }

    override suspend fun saveAppSettings(settings: AppSettingsEntity) {
        appSettingsDao.insertOrUpdate(settings)
    }

    override suspend fun updateTimeLimit(packageName: String, limitMinutes: Int) {
        appSettingsDao.updateTimeLimit(packageName, limitMinutes)
    }

    override suspend fun updateExclusion(packageName: String, isExcluded: Boolean, confirmCount: Int) {
        appSettingsDao.updateExclusion(packageName, isExcluded, confirmCount)
    }

    override suspend fun updateLastNotificationTime(packageName: String, time: Long) {
        appSettingsDao.updateLastNotificationTime(packageName, time)
    }

    override suspend fun updateNotificationEnabled(packageName: String, enabled: Boolean) {
        val settings = appSettingsDao.getSettingsByPackage(packageName)
        if (settings != null && settings.isAddictive && !enabled) {
            // For addictive apps, increment confirm count instead of directly disabling
            val newCount = settings.unsubscribeConfirmCount + 1
            if (newCount >= 3) {
                // After 3 confirmations, disable notifications and reset count
                appSettingsDao.updateNotificationEnabledAndResetConfirm(packageName, false)
            } else {
                appSettingsDao.updateUnsubscribeConfirmCount(packageName, newCount)
            }
        } else {
            appSettingsDao.updateNotificationEnabled(packageName, enabled)
        }
    }

    override suspend fun updateAddictive(packageName: String, isAddictive: Boolean, overrideUntil: Long) {
        appSettingsDao.updateAddictive(packageName, isAddictive, overrideUntil)
    }

    override suspend fun updateAddedTime(packageName: String, minutes: Int, increment: Int) {
        appSettingsDao.updateAddedTime(packageName, minutes, increment)
    }

    override suspend fun updateUnsubscribeConfirmCount(packageName: String, count: Int) {
        appSettingsDao.updateUnsubscribeConfirmCount(packageName, count)
    }

    override suspend fun resetAddedTimeForApp(packageName: String) {
        appSettingsDao.updateAddedTime(packageName, 0, 5) // Reset to default increment of 5
    }

    override fun getAddictiveApps(): Flow<List<AppSettingsEntity>> {
        return appSettingsDao.getAddictiveApps()
    }

    override fun getTodayUsage(): Flow<List<DailyUsageEntity>> {
        val today = LocalDate.now().format(dateFormatter)
        return dailyUsageDao.getUsageByDate(today)
    }

    override fun getTotalUsageToday(): Flow<Long?> {
        val today = LocalDate.now().format(dateFormatter)
        return dailyUsageDao.getTotalUsageByDate(today)
    }

    override suspend fun getUsageForDate(date: String): List<DailyUsageEntity> {
        return dailyUsageDao.getUsageByDateSync(date)
    }

    override suspend fun getTodayUsageForPackage(packageName: String): Long {
        val today = LocalDate.now().format(dateFormatter)
        val usage = dailyUsageDao.getUsageByPackageAndDate(packageName, today)
        return usage?.totalUsageSeconds ?: 0L
    }

    override suspend fun getUsageBetweenDates(startDate: String, endDate: String): List<DailyUsageEntity> {
        return dailyUsageDao.getUsageBetweenDatesSync(startDate, endDate)
    }

    override suspend fun recordUsageTime(
        packageName: String,
        appName: String,
        seconds: Long,
        isLateNight: Boolean
    ) {
        val today = LocalDate.now().format(dateFormatter)
        val existing = dailyUsageDao.getUsageByPackageAndDate(packageName, today)

        if (existing != null) {
            dailyUsageDao.addUsageTime(
                packageName = packageName,
                date = today,
                seconds = seconds,
                lateNightSeconds = if (isLateNight) seconds else 0
            )
        } else {
            dailyUsageDao.insertOrUpdate(
                DailyUsageEntity(
                    packageName = packageName,
                    appName = appName,
                    date = today,
                    totalUsageSeconds = seconds,
                    openCount = 1,
                    shortSessionCount = 0,
                    lateNightUsageSeconds = if (isLateNight) seconds else 0
                )
            )
        }
    }

    override suspend fun incrementOpenCount(packageName: String, appName: String) {
        val today = LocalDate.now().format(dateFormatter)
        val existing = dailyUsageDao.getUsageByPackageAndDate(packageName, today)

        if (existing != null) {
            dailyUsageDao.incrementOpenCount(packageName, today)
        } else {
            dailyUsageDao.insertOrUpdate(
                DailyUsageEntity(
                    packageName = packageName,
                    appName = appName,
                    date = today,
                    totalUsageSeconds = 0,
                    openCount = 1,
                    shortSessionCount = 0,
                    lateNightUsageSeconds = 0
                )
            )
        }
    }

    override suspend fun incrementShortSessionCount(packageName: String) {
        val today = LocalDate.now().format(dateFormatter)
        dailyUsageDao.incrementShortSessionCount(packageName, today)
    }

    override suspend fun saveSession(session: UsageSessionEntity) {
        usageSessionDao.insert(session)
    }

    override fun getRecentSessions(limit: Int): Flow<List<UsageSessionEntity>> {
        return usageSessionDao.getRecentSessions(limit)
    }

    override suspend fun getSessionsForDate(date: String, packageName: String): List<UsageSessionEntity> {
        return usageSessionDao.getSessionsByPackageAndDate(packageName, date)
    }

    override suspend fun getAddictionMetrics(packageName: String): AddictionMetrics {
        return addictionDetector.calculateAddictionMetrics(packageName)
    }

    override suspend fun getInstalledApps(): List<AppUsageInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)

        return resolveInfos.mapNotNull { resolveInfo ->
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val appName = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)

                val settings = appSettingsDao.getSettingsByPackage(packageName)
                val today = LocalDate.now().format(dateFormatter)
                val todayUsage = dailyUsageDao.getUsageByPackageAndDate(packageName, today)
                val addictionMetrics = addictionDetector.calculateAddictionMetrics(packageName)

                AppUsageInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    todayUsageSeconds = todayUsage?.totalUsageSeconds ?: 0,
                    timeLimitMinutes = settings?.timeLimitMinutes ?: 15,
                    isExcluded = settings?.isExcluded ?: false,
                    isAddictive = addictionMetrics.isAddictive,
                    openCount = todayUsage?.openCount ?: 0
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.todayUsageSeconds }
    }

    override suspend fun syncInstalledApps() {
        val installedApps = getInstalledApps()
        val existingSettings = appSettingsDao.getAllSettings()

        installedApps.forEach { app ->
            val existing = appSettingsDao.getSettingsByPackage(app.packageName)
            if (existing == null) {
                appSettingsDao.insertOrUpdate(
                    AppSettingsEntity(
                        packageName = app.packageName,
                        appName = app.appName,
                        timeLimitMinutes = 15,
                        isExcluded = false,
                        exclusionConfirmCount = 0
                    )
                )
            }
        }
    }

    override suspend fun cleanupOldData(daysToKeep: Int) {
        val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong()).format(dateFormatter)
        dailyUsageDao.deleteOlderThan(cutoffDate)
        usageSessionDao.deleteOlderThan(cutoffDate)
    }
}
