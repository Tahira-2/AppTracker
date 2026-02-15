package com.alertsystem.apptracker.ui.screens.analytics

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertsystem.apptracker.data.local.entity.DailyUsageEntity
import com.alertsystem.apptracker.data.model.AddictionMetrics
import com.alertsystem.apptracker.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DailyStats(
    val date: String,
    val dayOfWeek: String,
    val totalSeconds: Long
)

data class AppAnalytics(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val totalUsageSeconds: Long,
    val avgDailySeconds: Long,
    val totalOpens: Int,
    val metrics: AddictionMetrics
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val weeklyStats: List<DailyStats> = emptyList(),
    val topApps: List<AppAnalytics> = emptyList(),
    val addictiveApps: List<AppAnalytics> = emptyList(),
    val totalWeeklyUsageSeconds: Long = 0,
    val avgDailyUsageSeconds: Long = 0
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE")

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = LocalDate.now()
            val weekAgo = today.minusDays(6)

            val startDate = weekAgo.format(dateFormatter)
            val endDate = today.format(dateFormatter)

            // Get usage data for the week
            val weeklyUsage = repository.getUsageBetweenDates(startDate, endDate)

            // Calculate daily stats
            val dailyStats = (0..6).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val dateStr = date.format(dateFormatter)
                val dayUsage = weeklyUsage.filter { it.date == dateStr }
                    .sumOf { it.totalUsageSeconds }

                DailyStats(
                    date = dateStr,
                    dayOfWeek = date.format(dayOfWeekFormatter),
                    totalSeconds = dayUsage
                )
            }.reversed()

            // Calculate app analytics
            val appUsageMap = weeklyUsage.groupBy { it.packageName }
            val appAnalyticsList = appUsageMap.mapNotNull { (packageName, usageList) ->
                try {
                    val totalSeconds = usageList.sumOf { it.totalUsageSeconds }
                    val totalOpens = usageList.sumOf { it.openCount }
                    val appName = usageList.firstOrNull()?.appName ?: packageName
                    val icon = getAppIcon(packageName)
                    val metrics = repository.getAddictionMetrics(packageName)

                    AppAnalytics(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                        totalUsageSeconds = totalSeconds,
                        avgDailySeconds = totalSeconds / 7,
                        totalOpens = totalOpens,
                        metrics = metrics
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.totalUsageSeconds }

            val totalWeekly = dailyStats.sumOf { it.totalSeconds }
            val avgDaily = if (dailyStats.isNotEmpty()) totalWeekly / dailyStats.size else 0

            _uiState.value = AnalyticsUiState(
                isLoading = false,
                weeklyStats = dailyStats,
                topApps = appAnalyticsList.take(10),
                addictiveApps = appAnalyticsList.filter { it.metrics.isAddictive },
                totalWeeklyUsageSeconds = totalWeekly,
                avgDailyUsageSeconds = avgDaily
            )
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    fun formatHours(seconds: Long): String {
        val hours = seconds / 3600.0
        return String.format("%.1fh", hours)
    }
}
