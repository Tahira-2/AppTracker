package com.alertsystem.apptracker.ui.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertsystem.apptracker.data.local.entity.AppSettingsEntity
import com.alertsystem.apptracker.data.model.AddictionMetrics
import com.alertsystem.apptracker.domain.repository.UsageRepository
import com.alertsystem.apptracker.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettingsItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val timeLimitMinutes: Int,
    val isExcluded: Boolean,
    val exclusionConfirmCount: Int,
    val isAddictive: Boolean,
    val addictionScore: Int,
    val isNotificationEnabled: Boolean,
    val unsubscribeConfirmCount: Int,
    val isWork: Boolean
)

data class AppSettingsUiState(
    val isLoading: Boolean = true,
    val apps: List<AppSettingsItem> = emptyList(),
    val searchQuery: String = "",
    val showExclusionDialog: Boolean = false,
    val selectedApp: AppSettingsItem? = null,
    val confirmationStep: Int = 0,
    val showUnsubscribeDialog: Boolean = false,
    val pendingUnsubscribeApp: AppSettingsItem? = null,
    val showUnmarkAddictiveDialog: Boolean = false,
    val pendingUnmarkApp: AppSettingsItem? = null
)

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // One-time snapshot instead of continuous Flow collection.
            // This prevents apps from jumping position when settings change.
            // List re-sorts only when the page is reloaded or app is reopened.
            val settingsList = repository.getAllAppSettings().first()

            val appItems = settingsList.mapNotNull { settings ->
                try {
                    val icon = getAppIcon(settings.packageName)
                    val metrics = repository.getAddictionMetrics(settings.packageName)

                    AppSettingsItem(
                        packageName = settings.packageName,
                        appName = settings.appName,
                        icon = icon,
                        timeLimitMinutes = settings.timeLimitMinutes,
                        isExcluded = settings.isExcluded,
                        exclusionConfirmCount = settings.exclusionConfirmCount,
                        isAddictive = if (settings.addictiveOverrideUntil > System.currentTimeMillis()) settings.isAddictive else (settings.isAddictive || metrics.isAddictive),
                        addictionScore = metrics.addictionScore,
                        isNotificationEnabled = settings.isNotificationEnabled,
                        unsubscribeConfirmCount = settings.unsubscribeConfirmCount,
                        isWork = settings.isWork
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedWith(
                compareByDescending<AppSettingsItem> { it.isNotificationEnabled }
                    .thenByDescending { it.isAddictive }
                    .thenBy { it.appName }
            )

            _uiState.value = _uiState.value.copy(
                apps = appItems,
                isLoading = false
            )
        }
    }

    private fun updateAppInPlace(packageName: String, transform: (AppSettingsItem) -> AppSettingsItem) {
        val updatedApps = _uiState.value.apps.map {
            if (it.packageName == packageName) transform(it) else it
        }
        _uiState.value = _uiState.value.copy(apps = updatedApps)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateTimeLimit(packageName: String, minutes: Int) {
        viewModelScope.launch {
            repository.updateTimeLimit(packageName, minutes)
            updateAppInPlace(packageName) { it.copy(timeLimitMinutes = minutes) }
        }
    }

    fun onExclusionToggleClicked(app: AppSettingsItem) {
        if (app.isExcluded) {
            // Simply re-enable notifications
            viewModelScope.launch {
                repository.updateExclusion(app.packageName, false, 0)
                updateAppInPlace(app.packageName) { it.copy(isExcluded = false) }
            }
        } else {
            // Show exclusion dialog
            _uiState.value = _uiState.value.copy(
                showExclusionDialog = true,
                selectedApp = app,
                confirmationStep = 1
            )
        }
    }

    fun confirmExclusion() {
        val app = _uiState.value.selectedApp ?: return
        val currentStep = _uiState.value.confirmationStep
        val requiredConfirmations = if (app.isAddictive) {
            Constants.ADDICTIVE_APP_CONFIRM_COUNT
        } else {
            Constants.NORMAL_APP_CONFIRM_COUNT
        }

        if (currentStep < requiredConfirmations) {
            // Need more confirmations
            _uiState.value = _uiState.value.copy(
                confirmationStep = currentStep + 1
            )
        } else {
            // All confirmations received, exclude the app
            viewModelScope.launch {
                repository.updateExclusion(app.packageName, true, currentStep)
                updateAppInPlace(app.packageName) { it.copy(isExcluded = true) }
            }
            dismissExclusionDialog()
        }
    }

    fun dismissExclusionDialog() {
        _uiState.value = _uiState.value.copy(
            showExclusionDialog = false,
            selectedApp = null,
            confirmationStep = 0
        )
    }

    fun getFilteredApps(): List<AppSettingsItem> {
        val query = _uiState.value.searchQuery.lowercase()
        return if (query.isEmpty()) {
            _uiState.value.apps
        } else {
            _uiState.value.apps.filter {
                it.appName.lowercase().contains(query)
            }
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.syncInstalledApps()
            loadApps()
        }
    }

    fun onNotificationToggle(app: AppSettingsItem, enabled: Boolean) {
        if (!enabled && app.isAddictive) {
            // Show unsubscribe dialog for addictive apps
            _uiState.value = _uiState.value.copy(
                showUnsubscribeDialog = true,
                pendingUnsubscribeApp = app
            )
        } else {
            viewModelScope.launch {
                val settings = repository.getAppSettings(app.packageName)
                if (settings != null) {
                    repository.saveAppSettings(
                        settings.copy(
                            isNotificationEnabled = enabled,
                            unsubscribeConfirmCount = 0,
                            // Reset added time when enabling notifications
                            addedTimeMinutes = if (enabled) 0 else settings.addedTimeMinutes,
                            currentAddTimeIncrement = if (enabled) 5 else settings.currentAddTimeIncrement
                        )
                    )
                    updateAppInPlace(app.packageName) { it.copy(isNotificationEnabled = enabled) }
                }
            }
        }
    }

    fun confirmUnsubscribeAddictive() {
        val app = _uiState.value.pendingUnsubscribeApp ?: return
        viewModelScope.launch {
            val settings = repository.getAppSettings(app.packageName)
            if (settings != null) {
                repository.saveAppSettings(
                    settings.copy(isNotificationEnabled = false)
                )
                updateAppInPlace(app.packageName) { it.copy(isNotificationEnabled = false) }
            }
        }
        dismissUnsubscribeDialog()
    }

    fun dismissUnsubscribeDialog() {
        _uiState.value = _uiState.value.copy(
            showUnsubscribeDialog = false,
            pendingUnsubscribeApp = null
        )
    }

    fun toggleAddictive(app: AppSettingsItem, isAddictive: Boolean) {
        if (!isAddictive && app.isAddictive) {
            // User is trying to unmark as addictive - show warning
            _uiState.value = _uiState.value.copy(
                showUnmarkAddictiveDialog = true,
                pendingUnmarkApp = app
            )
        } else {
            viewModelScope.launch {
                repository.updateAddictive(app.packageName, isAddictive)
                updateAppInPlace(app.packageName) { it.copy(isAddictive = isAddictive) }
            }
        }
    }

    fun confirmUnmarkAddictive() {
        val app = _uiState.value.pendingUnmarkApp ?: return
        viewModelScope.launch {
            val overrideUntil = System.currentTimeMillis() + Constants.ADDICTIVE_OVERRIDE_DURATION_MS
            repository.updateAddictive(app.packageName, false, overrideUntil = overrideUntil)
            updateAppInPlace(app.packageName) { it.copy(isAddictive = false) }
        }
        dismissUnmarkAddictiveDialog()
    }

    fun toggleWork(packageName: String, isWork: Boolean) {
        viewModelScope.launch {
            val settings = repository.getAppSettings(packageName)
            if (settings != null) {
                repository.saveAppSettings(settings.copy(isWork = isWork))
                if (isWork && settings.isAddictive) {
                    // If marking as work and currently addictive, remove addictive label
                    val overrideUntil = System.currentTimeMillis() + Constants.ADDICTIVE_OVERRIDE_DURATION_MS
                    repository.updateAddictive(packageName, false, overrideUntil = overrideUntil)
                    updateAppInPlace(packageName) { it.copy(isWork = true, isAddictive = false) }
                } else {
                    updateAppInPlace(packageName) { it.copy(isWork = isWork) }
                }
            }
        }
    }

    fun dismissUnmarkAddictiveDialog() {
        _uiState.value = _uiState.value.copy(
            showUnmarkAddictiveDialog = false,
            pendingUnmarkApp = null
        )
    }
}
