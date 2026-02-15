package com.alertsystem.apptracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.alertsystem.apptracker.receiver.WeeklyReportReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyReportScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WeeklyReportScheduler"
        private const val REQUEST_CODE = 1001
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleWeeklyReport() {
        val intent = Intent(context, WeeklyReportReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate next Sunday at 9:00 AM
        val calendar = Calendar.getInstance().apply {
            // Move to next Sunday
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            // If it's Sunday but past 9 AM, move to next Sunday
            if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                val now = Calendar.getInstance()
                if (now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY &&
                    (now.get(Calendar.HOUR_OF_DAY) > 9 ||
                            (now.get(Calendar.HOUR_OF_DAY) == 9 && now.get(Calendar.MINUTE) > 0))) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val triggerTime = calendar.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fall back to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "Weekly report scheduled for: ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule weekly report", e)
        }
    }

    fun cancelWeeklyReport() {
        val intent = Intent(context, WeeklyReportReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Weekly report cancelled")
    }
}
