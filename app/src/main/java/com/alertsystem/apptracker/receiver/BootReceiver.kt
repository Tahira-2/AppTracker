package com.alertsystem.apptracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alertsystem.apptracker.service.UsageTrackingService
import com.alertsystem.apptracker.util.WeeklyReportScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var weeklyReportScheduler: WeeklyReportScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            UsageTrackingService.start(context)
            weeklyReportScheduler.scheduleWeeklyReport()
        }
    }
}
