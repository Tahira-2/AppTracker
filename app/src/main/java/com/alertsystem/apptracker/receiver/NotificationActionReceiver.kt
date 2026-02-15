package com.alertsystem.apptracker.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alertsystem.apptracker.util.Constants

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(Constants.EXTRA_NOTIFICATION_ID, -1)
        val packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE_NAME)

        when (intent.action) {
            Constants.ACTION_DISMISS_NOTIFICATION -> {
                if (notificationId != -1) {
                    val notificationManager = context.getSystemService(NotificationManager::class.java)
                    notificationManager.cancel(notificationId)
                }
            }
            Constants.ACTION_SNOOZE_NOTIFICATION -> {
                if (notificationId != -1) {
                    val notificationManager = context.getSystemService(NotificationManager::class.java)
                    notificationManager.cancel(notificationId)

                    // Note: Snooze functionality is handled by the service via SharedPreferences
                    // The service checks snooze state before sending notifications
                    packageName?.let {
                        val prefs = context.getSharedPreferences("snooze_prefs", Context.MODE_PRIVATE)
                        val snoozeUntil = System.currentTimeMillis() + (Constants.SNOOZE_DURATION_MINUTES * 60 * 1000)
                        prefs.edit().putLong(it, snoozeUntil).apply()
                    }
                }
            }
        }
    }
}
