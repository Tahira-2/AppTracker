package com.alertsystem.apptracker.util

import com.alertsystem.apptracker.data.local.dao.DailyUsageDao
import com.alertsystem.apptracker.data.model.AddictionMetrics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddictionDetector @Inject constructor(
    private val dailyUsageDao: DailyUsageDao
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        // Thresholds for addiction detection
        const val HIGH_USAGE_THRESHOLD_SECONDS = 2 * 60 * 60 // 2 hours daily average
        const val FREQUENT_OPENS_THRESHOLD = 20 // 20 opens per day average
        const val COMPULSIVE_CHECKING_THRESHOLD = 10 // 10 short sessions per day
        const val LATE_NIGHT_USAGE_THRESHOLD_SECONDS = 30 * 60 // 30 minutes late night usage

        // Number of days to analyze
        const val ANALYSIS_PERIOD_DAYS = 7

        // Minimum criteria to meet for addiction classification
        const val ADDICTION_THRESHOLD = 2

        // TESTING: Mark as addictive if used more than 1 hour in a single day
        const val SINGLE_DAY_ADDICTIVE_THRESHOLD_SECONDS = 60 * 60 // 1 hour
    }

    suspend fun calculateAddictionMetrics(packageName: String): AddictionMetrics {
        val today = LocalDate.now().format(dateFormatter)
        val endDate = today
        val startDate = LocalDate.now().minusDays(ANALYSIS_PERIOD_DAYS.toLong()).format(dateFormatter)

        val avgDailyUsage = dailyUsageDao.getAverageUsageForPackage(packageName, startDate, endDate) ?: 0.0
        val avgOpenCount = dailyUsageDao.getAverageOpenCountForPackage(packageName, startDate, endDate) ?: 0.0
        val avgShortSessions = dailyUsageDao.getAverageShortSessionsForPackage(packageName, startDate, endDate) ?: 0.0
        val avgLateNightUsage = dailyUsageDao.getAverageLateNightUsageForPackage(packageName, startDate, endDate) ?: 0.0

        // TESTING: Check if used more than 1 hour today
        val todayUsage = dailyUsageDao.getUsageByPackageAndDate(packageName, today)
        val todayUsageSeconds = todayUsage?.totalUsageSeconds ?: 0

        var addictionScore = 0

        // TESTING CRITERION: More than 1 hour usage in a single day = addictive
        val exceededSingleDayThreshold = todayUsageSeconds > SINGLE_DAY_ADDICTIVE_THRESHOLD_SECONDS
        if (exceededSingleDayThreshold) {
            addictionScore += 2 // Automatically meets threshold
        }

        // Criterion 1: High usage (>2 hours daily average)
        if (avgDailyUsage > HIGH_USAGE_THRESHOLD_SECONDS) {
            addictionScore++
        }

        // Criterion 2: Frequent opens (>20 opens per day)
        if (avgOpenCount > FREQUENT_OPENS_THRESHOLD) {
            addictionScore++
        }

        // Criterion 3: Compulsive checking (>10 short sessions per day)
        if (avgShortSessions > COMPULSIVE_CHECKING_THRESHOLD) {
            addictionScore++
        }

        // Criterion 4: Late night usage (>30 min between 11 PM - 6 AM)
        if (avgLateNightUsage > LATE_NIGHT_USAGE_THRESHOLD_SECONDS) {
            addictionScore++
        }

        val isAddictive = addictionScore >= ADDICTION_THRESHOLD

        return AddictionMetrics(
            packageName = packageName,
            avgDailyUsageSeconds = avgDailyUsage,
            avgOpenCount = avgOpenCount,
            avgShortSessions = avgShortSessions,
            avgLateNightUsageSeconds = avgLateNightUsage,
            isAddictive = isAddictive,
            addictionScore = addictionScore
        )
    }

    /**
     * Check if app exceeded 1 hour usage today (for testing purposes)
     */
    suspend fun checkAndMarkAddictiveIfNeeded(packageName: String): Boolean {
        val today = LocalDate.now().format(dateFormatter)
        val todayUsage = dailyUsageDao.getUsageByPackageAndDate(packageName, today)
        val todayUsageSeconds = todayUsage?.totalUsageSeconds ?: 0
        return todayUsageSeconds > SINGLE_DAY_ADDICTIVE_THRESHOLD_SECONDS
    }

    fun isLateNightTime(): Boolean {
        val hour = java.time.LocalTime.now().hour
        return hour >= 23 || hour < 6
    }
}
