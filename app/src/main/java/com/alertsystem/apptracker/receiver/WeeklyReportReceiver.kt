package com.alertsystem.apptracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alertsystem.apptracker.domain.repository.UsageRepository
import com.alertsystem.apptracker.service.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class WeeklyReportReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WeeklyReportReceiver"
    }

    @Inject
    lateinit var repository: UsageRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Weekly report alarm triggered")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reportData = generateWeeklyReport()
                notificationHelper.showWeeklyReportNotification(reportData)
                Log.d(TAG, "Weekly report notification sent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate weekly report", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun generateWeeklyReport(): WeeklyReportData {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)

        val startDate = weekAgo.format(dateFormatter)
        val endDate = today.format(dateFormatter)

        // Get total usage for the week
        val allUsage = repository.getUsageBetweenDates(startDate, endDate)

        val totalUsageSeconds = allUsage.sumOf { it.totalUsageSeconds }
        val totalOpenCount = allUsage.sumOf { it.openCount }

        // Get top 5 most used apps
        val appUsageMap = mutableMapOf<String, Long>()
        val appNameMap = mutableMapOf<String, String>()
        for (usage in allUsage) {
            appUsageMap[usage.packageName] = (appUsageMap[usage.packageName] ?: 0L) + usage.totalUsageSeconds
            appNameMap[usage.packageName] = usage.appName
        }

        val topApps = appUsageMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { TopAppUsage(appNameMap[it.key] ?: it.key, it.value) }

        // Get addictive apps count
        val addictiveApps = repository.getAddictiveApps().first()

        return WeeklyReportData(
            totalUsageSeconds = totalUsageSeconds,
            totalOpenCount = totalOpenCount,
            topApps = topApps,
            addictiveAppsCount = addictiveApps.size
        )
    }
}

data class WeeklyReportData(
    val totalUsageSeconds: Long,
    val totalOpenCount: Int,
    val topApps: List<TopAppUsage>,
    val addictiveAppsCount: Int
)

data class TopAppUsage(
    val appName: String,
    val usageSeconds: Long
)
