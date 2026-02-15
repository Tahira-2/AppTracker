package com.alertsystem.apptracker.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alertsystem.apptracker.domain.repository.UsageRepository
import com.alertsystem.apptracker.util.Constants
import com.alertsystem.apptracker.ui.alert.UsageAlertActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlertResponseReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: UsageRepository

    companion object {
        private const val TAG = "AlertResponseReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action") ?: return
        val packageName = intent.getStringExtra("package_name") ?: return

        Log.d(TAG, "Received alert response: action=$action, package=$packageName")

        // Dismiss the ongoing notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notificationId = Constants.NOTIFICATION_ID_ALERT_BASE + packageName.hashCode()
        notificationManager.cancel(notificationId)

        when (action) {
            UsageAlertActivity.RESULT_IGNORE -> {
                handleIgnore(packageName)
            }
            UsageAlertActivity.RESULT_ADD_TIME -> {
                val addMinutes = intent.getIntExtra("add_minutes", 5)
                handleAddTime(packageName, addMinutes)
            }
        }
    }

    private fun handleIgnore(packageName: String) {
        Log.d(TAG, "User ignored alert for $packageName - will remind again after original interval")
        // Reset added time and increment for next reminder
        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getAppSettings(packageName)
            if (settings != null) {
                // Reset the added time and increment when user ignores
                repository.updateAddedTime(packageName, 0, 5)
                // Update last notification time so it reminds again after the original interval
                repository.updateLastNotificationTime(packageName, System.currentTimeMillis())
            }
        }
    }

    private fun handleAddTime(packageName: String, addMinutes: Int) {
        Log.d(TAG, "User added $addMinutes minutes for $packageName")
        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getAppSettings(packageName)
            if (settings != null) {
                // Add the time
                val newAddedTime = settings.addedTimeMinutes + addMinutes
                // Increase the increment for next time (5 -> 10 -> 15 -> ...)
                val newIncrement = settings.currentAddTimeIncrement + 5
                repository.updateAddedTime(packageName, newAddedTime, newIncrement)
                // Update last notification time
                repository.updateLastNotificationTime(packageName, System.currentTimeMillis())
            }
        }
    }
}
