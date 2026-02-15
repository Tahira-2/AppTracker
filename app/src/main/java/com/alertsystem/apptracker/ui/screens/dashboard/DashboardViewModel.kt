package com.alertsystem.apptracker.ui.screens.dashboard

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertsystem.apptracker.data.local.entity.DailyUsageEntity
import com.alertsystem.apptracker.domain.repository.UsageRepository
import com.alertsystem.apptracker.service.UsageTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalUsageToday: Long = 0,
    val appUsageList: List<DailyUsageEntity> = emptyList(),
    val hasUsagePermission: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        observeUsageData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            val hasPermission = hasUsageStatsPermission()
            _uiState.value = _uiState.value.copy(
                hasUsagePermission = hasPermission,
                isLoading = false
            )

            // Auto-start tracking when permission is granted
            if (hasPermission) {
                UsageTrackingService.start(context)
            }
        }
    }

    private fun observeUsageData() {
        viewModelScope.launch {
            repository.getTodayUsage().collect { usageList ->
                val totalUsage = usageList.sumOf { it.totalUsageSeconds }
                _uiState.value = _uiState.value.copy(
                    appUsageList = usageList,
                    totalUsageToday = totalUsage,
                    isLoading = false
                )
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.syncInstalledApps()
            val hasPermission = hasUsageStatsPermission()
            _uiState.value = _uiState.value.copy(
                hasUsagePermission = hasPermission,
                isLoading = false
            )

            // Auto-start tracking when permission is granted
            if (hasPermission) {
                UsageTrackingService.start(context)
            }
        }
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
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
}
