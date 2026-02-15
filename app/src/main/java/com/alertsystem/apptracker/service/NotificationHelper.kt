package com.alertsystem.apptracker.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alertsystem.apptracker.MainActivity
import com.alertsystem.apptracker.MainApplication
import com.alertsystem.apptracker.R
import com.alertsystem.apptracker.receiver.NotificationActionReceiver
import com.alertsystem.apptracker.receiver.WeeklyReportData
import com.alertsystem.apptracker.ui.alert.UsageAlertActivity
import com.alertsystem.apptracker.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun createServiceNotification(): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, MainApplication.SERVICE_CHANNEL_ID)
            .setContentTitle("App Tracker Active")
            .setContentText("Monitoring your app usage")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showUsageAlert(packageName: String, appName: String, usageMinutes: Int) {
        val notificationId = Constants.NOTIFICATION_ID_ALERT_BASE + packageName.hashCode()

        // Main tap action - open app
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = Constants.ACTION_DISMISS_NOTIFICATION
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
            putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = Constants.ACTION_SNOOZE_NOTIFICATION
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
            putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MainApplication.ALERT_CHANNEL_ID)
            .setContentTitle("Usage Alert")
            .setContentText("You've been using $appName for $usageMinutes minutes. Time to take a break.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You've been using $appName for $usageMinutes minutes. Time to take a break.")
            )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Snooze ${Constants.SNOOZE_DURATION_MINUTES}m",
                snoozePendingIntent
            )
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showFullScreenAlert(
        packageName: String,
        appName: String,
        usageMinutes: Int,
        timeLimit: Int,
        addTimeIncrement: Int
    ) {
        val notificationId = Constants.NOTIFICATION_ID_ALERT_BASE + packageName.hashCode()

        Log.d("NotificationHelper", "Creating full-screen alert for $appName ($usageMinutes min)")

        // Create intent for the full-screen alert activity
        val alertIntent = UsageAlertActivity.createIntent(
            context = context,
            packageName = packageName,
            appName = appName,
            usageMinutes = usageMinutes,
            timeLimit = timeLimit,
            addTimeIncrement = addTimeIncrement
        )

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with full-screen intent
        // On unlocked phone: this launches the activity directly over the current app
        // On locked phone: shows as heads-up notification
        val notification = NotificationCompat.Builder(context, MainApplication.ALERT_CHANNEL_ID)
            .setContentTitle("Time Limit Reached")
            .setContentText("You've been using $appName for $usageMinutes minutes")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true) // Prevent swipe dismiss
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAllAlerts() {
        notificationManager.cancelAll()
    }

    fun showWeeklyReportNotification(reportData: WeeklyReportData) {
        val notificationId = Constants.NOTIFICATION_ID_WEEKLY_REPORT

        // Open app on tap
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format usage time
        val totalHours = reportData.totalUsageSeconds / 3600
        val totalMinutes = (reportData.totalUsageSeconds % 3600) / 60
        val usageText = if (totalHours > 0) {
            "${totalHours}h ${totalMinutes}m"
        } else {
            "${totalMinutes}m"
        }

        // Format top apps
        val topAppsText = if (reportData.topApps.isNotEmpty()) {
            reportData.topApps.take(3).joinToString(", ") { app ->
                val hours = app.usageSeconds / 3600
                val mins = (app.usageSeconds % 3600) / 60
                val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                "${app.appName}: $timeStr"
            }
        } else {
            "No usage data"
        }

        val summaryText = StringBuilder().apply {
            append("Total screen time: $usageText\n")
            append("Apps opened: ${reportData.totalOpenCount} times\n")
            append("Top apps: $topAppsText")
            if (reportData.addictiveAppsCount > 0) {
                append("\n⚠️ ${reportData.addictiveAppsCount} addictive app${if (reportData.addictiveAppsCount > 1) "s" else ""} detected")
            }
        }.toString()

        val notification = NotificationCompat.Builder(context, MainApplication.ALERT_CHANNEL_ID)
            .setContentTitle("📊 Weekly Usage Report")
            .setContentText("Total screen time: $usageText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(summaryText)
            )
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
