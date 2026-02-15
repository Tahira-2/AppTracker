package com.alertsystem.apptracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.alertsystem.apptracker.util.WeeklyReportScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class MainApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WeeklyReportSchedulerEntryPoint {
        fun weeklyReportScheduler(): WeeklyReportScheduler
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleWeeklyReport()
    }

    private fun scheduleWeeklyReport() {
        val entryPoint = EntryPointAccessors.fromApplication(
            this,
            WeeklyReportSchedulerEntryPoint::class.java
        )
        entryPoint.weeklyReportScheduler().scheduleWeeklyReport()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Usage alerts channel
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
            enableVibration(true)
        }

        // Service channel (low importance, persistent)
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.service_channel_description)
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(alertChannel)
        notificationManager.createNotificationChannel(serviceChannel)
    }

    companion object {
        const val ALERT_CHANNEL_ID = "usage_alerts"
        const val SERVICE_CHANNEL_ID = "tracking_service"
    }
}
