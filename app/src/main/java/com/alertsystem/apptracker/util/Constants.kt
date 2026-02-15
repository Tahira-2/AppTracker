package com.alertsystem.apptracker.util

object Constants {
    // Tracking intervals
    const val TRACKING_INTERVAL_MS = 5000L // 5 seconds
    const val GRACE_PERIOD_MS = 30000L // 30 seconds grace period

    // Default settings
    const val DEFAULT_TIME_LIMIT_MINUTES = 1 // 1 minute for testing - change to 15 for production
    const val MIN_TIME_LIMIT_MINUTES = 1
    const val MAX_TIME_LIMIT_MINUTES = 60

    // Notification settings
    const val NOTIFICATION_ID_SERVICE = 1001
    const val NOTIFICATION_ID_ALERT_BASE = 2000
    const val NOTIFICATION_ID_WEEKLY_REPORT = 3001

    // Session thresholds
    const val SHORT_SESSION_THRESHOLD_SECONDS = 120 // 2 minutes

    // Data retention
    const val DATA_RETENTION_DAYS = 30

    // Exclusion confirmation requirements
    const val ADDICTIVE_APP_CONFIRM_COUNT = 3
    const val NORMAL_APP_CONFIRM_COUNT = 1

    // Intent actions
    const val ACTION_DISMISS_NOTIFICATION = "com.alertsystem.apptracker.DISMISS"
    const val ACTION_SNOOZE_NOTIFICATION = "com.alertsystem.apptracker.SNOOZE"
    const val EXTRA_PACKAGE_NAME = "package_name"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    // Snooze duration
    const val SNOOZE_DURATION_MINUTES = 5
}
