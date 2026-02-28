package com.alertsystem.apptracker.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.alertsystem.apptracker.data.local.entity.UsageSessionEntity
import com.alertsystem.apptracker.domain.repository.UsageRepository
import com.alertsystem.apptracker.util.AddictionDetector
import com.alertsystem.apptracker.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class UsageTrackingService : Service() {

    companion object {
        private const val TAG = "UsageTrackingService"

        // Packages to ignore (launchers, system UI, etc.)
        private val IGNORED_PACKAGES = setOf(
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher",
            "com.android.launcher2",
            "com.android.launcher3",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.android.systemui",
            "com.android.settings",
            "com.alertsystem.apptracker" // Don't track our own app
        )

        fun start(context: Context) {
            Log.d(TAG, "Starting service...")
            val intent = Intent(context, UsageTrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            Log.d(TAG, "Stopping service...")
            val intent = Intent(context, UsageTrackingService::class.java)
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var repository: UsageRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var addictionDetector: AddictionDetector

    @Inject
    lateinit var alertOverlayManager: AlertOverlayManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    private var usageStatsManager: UsageStatsManager? = null
    private var packageManager: PackageManager? = null

    // Tracking state
    private var currentApp: String? = null
    private var currentAppName: String = ""
    private var sessionStartTime: Long = 0
    private var continuousUsageSeconds: Long = 0
    private var lastActiveTime: Long = 0
    private var isInGracePeriod: Boolean = false
    private var gracePeriodStartTime: Long = 0

    // Snooze tracking
    private val snoozedApps = mutableMapOf<String, Long>()

    // Tracks when we last saw a confirmed event for the current app
    private var lastConfirmedActiveTime: Long = 0

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val trackingRunnable = object : Runnable {
        override fun run() {
            trackCurrentApp()
            handler.postDelayed(this, Constants.TRACKING_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        packageManager = applicationContext.packageManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        startForeground(Constants.NOTIFICATION_ID_SERVICE, notificationHelper.createServiceNotification())
        handler.post(trackingRunnable)

        // Sync installed apps on start
        serviceScope.launch {
            repository.syncInstalledApps()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        handler.removeCallbacks(trackingRunnable)
        saveCurrentSession()
        serviceScope.cancel()
    }

    private fun trackCurrentApp() {
        val foregroundApp = getActivelyUsedApp()
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "Poll: activeApp=$foregroundApp, currentTracked=$currentApp, isGrace=$isInGracePeriod")

        when {
            // No app actively in foreground (home screen, screen off, etc.)
            foregroundApp == null -> {
                handleNoApp()
            }
            // Same app as before - continue tracking
            foregroundApp == currentApp -> {
                handleSameApp(currentTime)
            }
            // Different app detected - switch tracking
            else -> {
                handleNewApp(foregroundApp, currentTime)
            }
        }
    }

    private fun handleNoApp() {
        if (currentApp != null && !isInGracePeriod) {
            Log.d(TAG, "No active app - entering grace period for: $currentAppName")
            isInGracePeriod = true
            gracePeriodStartTime = System.currentTimeMillis()
            // Auto-dismiss overlay if user left the app
            alertOverlayManager.dismissIfShowingForPackage(currentApp!!)
        } else if (isInGracePeriod) {
            val currentTime = System.currentTimeMillis()
            val graceDuration = currentTime - gracePeriodStartTime

            if (graceDuration >= Constants.GRACE_PERIOD_MS) {
                Log.d(TAG, "Grace period expired (${graceDuration}ms), saving session for $currentAppName")
                val closedApp = currentApp
                saveCurrentSession()
                resetTrackingState()
                // Reset warning timer so next session starts fresh
                closedApp?.let { resetWarningState(it) }
            } else {
                Log.d(TAG, "In grace period: ${graceDuration}ms / ${Constants.GRACE_PERIOD_MS}ms for $currentAppName")
            }
        }
    }

    private fun handleSameApp(currentTime: Long) {
        if (isInGracePeriod) {
            Log.d(TAG, "User returned within grace period to $currentAppName")
            isInGracePeriod = false
        }

        // Calculate total continuous usage from session start time directly
        // (avoids accumulated integer division rounding errors that lose ~12 min per hour)
        val previousUsageSeconds = continuousUsageSeconds
        continuousUsageSeconds = (currentTime - sessionStartTime) / 1000
        val elapsedSeconds = continuousUsageSeconds - previousUsageSeconds
        lastActiveTime = currentTime

        val usageMinutes = continuousUsageSeconds / 60
        Log.d(TAG, "Tracking: $currentAppName, +${elapsedSeconds}s, Total: ${continuousUsageSeconds}s (${usageMinutes}min)")

        // Record usage to database
        if (elapsedSeconds > 0) {
            serviceScope.launch {
                currentApp?.let { pkg ->
                    repository.recordUsageTime(
                        packageName = pkg,
                        appName = currentAppName,
                        seconds = elapsedSeconds,
                        isLateNight = addictionDetector.isLateNightTime()
                    )
                }
            }
        }

        // Check if we should send notification
        checkAndNotify()
    }

    private fun handleNewApp(newApp: String, currentTime: Long) {
        // Save previous session if exists
        if (currentApp != null) {
            Log.d(TAG, "Switching from $currentAppName to ${getAppName(newApp)}")
            // Auto-dismiss overlay if user left the app it was shown for
            val previousApp = currentApp!!
            alertOverlayManager.dismissIfShowingForPackage(previousApp)
            saveCurrentSession()
            // Reset warning timer so next session starts fresh
            resetWarningState(previousApp)
        }

        // Start new session
        currentApp = newApp
        currentAppName = getAppName(newApp)
        sessionStartTime = currentTime
        continuousUsageSeconds = 0
        lastActiveTime = currentTime
        lastConfirmedActiveTime = currentTime
        isInGracePeriod = false

        Log.d(TAG, "Started tracking: $currentAppName ($newApp)")

        // Record app open and check if addictive
        serviceScope.launch {
            repository.incrementOpenCount(newApp, currentAppName)

            // Show warning if this app is marked as addictive
            val settings = repository.getAppSettings(newApp)
            if (settings?.isAddictive == true) {
                val dailyUsageSeconds = repository.getTodayUsageForPackage(newApp)
                val dailyUsageMinutes = (dailyUsageSeconds / 60).toInt()
                Log.d(TAG, ">>> SHOWING ADDICTIVE WARNING for $currentAppName (daily usage: ${dailyUsageMinutes}m) <<<")
                alertOverlayManager.showAddictiveWarning(
                    packageName = newApp,
                    appName = currentAppName,
                    dailyUsageMinutes = dailyUsageMinutes
                )
            }
        }
    }

    private fun saveCurrentSession() {
        val app = currentApp ?: return
        if (sessionStartTime == 0L) return

        val endTime = System.currentTimeMillis()
        val durationSeconds = (endTime - sessionStartTime) / 1000

        Log.d(TAG, "Saving session for $currentAppName: ${durationSeconds}s total")

        if (durationSeconds > 0) {
            serviceScope.launch {
                val session = UsageSessionEntity(
                    packageName = app,
                    appName = currentAppName,
                    startTime = sessionStartTime,
                    endTime = endTime,
                    durationSeconds = durationSeconds,
                    date = LocalDate.now().format(dateFormatter)
                )
                repository.saveSession(session)

                // Check for short session (compulsive checking)
                if (durationSeconds < Constants.SHORT_SESSION_THRESHOLD_SECONDS) {
                    repository.incrementShortSessionCount(app)
                }
            }
        }
    }

    private fun resetTrackingState() {
        Log.d(TAG, "Resetting tracking state")
        currentApp = null
        currentAppName = ""
        sessionStartTime = 0
        continuousUsageSeconds = 0
        lastActiveTime = 0
        lastConfirmedActiveTime = 0
        isInGracePeriod = false
        gracePeriodStartTime = 0
    }

    private fun resetWarningState(packageName: String) {
        Log.d(TAG, "Resetting warning state for $packageName")
        serviceScope.launch {
            repository.updateAddedTime(packageName, 0, 5)
            repository.updateLastNotificationTime(packageName, 0)
        }
    }

    private fun checkAndNotify() {
        val app = currentApp ?: return
        val usageMinutes = (continuousUsageSeconds / 60).toInt()

        serviceScope.launch {
            val settings = repository.getAppSettings(app)

            // Check for addictive status (Testing: >1 hour usage today)
            // Skip auto-marking for work apps
            if (settings?.isWork != true) {
                val shouldMarkAddictive = addictionDetector.checkAndMarkAddictiveIfNeeded(app)
                if (shouldMarkAddictive && settings?.isAddictive != true) {
                    Log.d(TAG, "Marking $currentAppName as addictive (>1 hour usage today)")
                    repository.updateAddictive(app, true)
                }
            }

            // Only send notifications for apps with notifications enabled
            if (settings?.isNotificationEnabled != true) {
                Log.d(TAG, "$currentAppName notifications not enabled, skipping alert")
                return@launch
            }

            // Check if excluded
            if (settings.isExcluded) {
                Log.d(TAG, "$currentAppName is excluded, skipping notification")
                return@launch
            }

            // Check if snoozed
            val snoozeUntil = snoozedApps[app]
            if (snoozeUntil != null && System.currentTimeMillis() < snoozeUntil) {
                Log.d(TAG, "$currentAppName is snoozed, skipping notification")
                return@launch
            }

            // Calculate effective time limit (base limit + added time from "Add Time" button)
            val baseTimeLimitMinutes = settings.timeLimitMinutes
            var addedTimeMinutes = settings.addedTimeMinutes

            // Daily reset: clear accumulated added time from previous days
            if (settings.lastNotificationTime > 0 && addedTimeMinutes > 0) {
                val lastNotificationDate = java.time.Instant.ofEpochMilli(settings.lastNotificationTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                if (lastNotificationDate != LocalDate.now()) {
                    Log.d(TAG, "New day - resetting addedTime for $currentAppName (was ${addedTimeMinutes}m)")
                    repository.updateAddedTime(app, 0, 5)
                    addedTimeMinutes = 0
                }
            }

            val effectiveTimeLimitMinutes = baseTimeLimitMinutes + addedTimeMinutes

            Log.d(TAG, "Notification check: $currentAppName - ${usageMinutes}min used / ${effectiveTimeLimitMinutes}min limit (base: $baseTimeLimitMinutes, added: $addedTimeMinutes)")

            if (usageMinutes >= effectiveTimeLimitMinutes) {
                // Check notification cooldown
                val lastNotification = settings.lastNotificationTime
                val notificationInterval = settings.notificationIntervalMinutes * 60 * 1000L
                val timeSinceLastNotification = System.currentTimeMillis() - lastNotification

                Log.d(TAG, "Time limit reached! Last notification was ${timeSinceLastNotification/1000}s ago, interval=${notificationInterval/1000}s")

                if (timeSinceLastNotification >= notificationInterval) {
                    Log.d(TAG, ">>> SHOWING OVERLAY ALERT for $currentAppName ($usageMinutes min) <<<")

                    // Show overlay popup directly over the current app
                    // Note: lastNotificationTime is updated inside showAlert only after
                    // the overlay is confirmed visible. This ensures retries if addView fails.
                    alertOverlayManager.showAlert(
                        packageName = app,
                        appName = currentAppName,
                        usageMinutes = usageMinutes,
                        timeLimit = effectiveTimeLimitMinutes,
                        addTimeIncrement = settings.currentAddTimeIncrement
                    )
                } else {
                    Log.d(TAG, "Notification on cooldown, ${(notificationInterval - timeSinceLastNotification)/1000}s remaining")
                }
            }
        }
    }

    /**
     * Gets the app that is ACTIVELY being used in the foreground.
     * Returns null if:
     * - Screen is off
     * - User is on home screen
     * - User is on a system UI app (launcher, settings, etc.)
     *
     * This ensures we only track apps the user is actively interacting with.
     */
    private fun getActivelyUsedApp(): String? {
        val currentTime = System.currentTimeMillis()

        // Query events from the last 5 minutes for reliable state detection
        val endTime = currentTime
        val startTime = currentTime - 300000 // 5 minutes

        val usageEvents = usageStatsManager?.queryEvents(startTime, endTime)

        // Track the state of apps: package -> (isResumed, timestamp)
        data class AppState(val isResumed: Boolean, val timestamp: Long)
        val appStates = mutableMapOf<String, AppState>()
        var screenInteractive = true
        var lastScreenEventTime: Long = 0

        usageEvents?.let { events ->
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        val current = appStates[event.packageName]
                        if (current == null || event.timeStamp > current.timestamp) {
                            appStates[event.packageName] = AppState(true, event.timeStamp)
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        val current = appStates[event.packageName]
                        // Only mark as paused if this event is more recent
                        if (current == null || event.timeStamp > current.timestamp) {
                            appStates[event.packageName] = AppState(false, event.timeStamp)
                        }
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        if (event.timeStamp > lastScreenEventTime) {
                            screenInteractive = false
                            lastScreenEventTime = event.timeStamp
                        }
                    }
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        if (event.timeStamp > lastScreenEventTime) {
                            screenInteractive = true
                            lastScreenEventTime = event.timeStamp
                        }
                    }
                }
            }
        }

        // If screen is off, no app is active
        if (!screenInteractive) {
            Log.d(TAG, "Screen is not interactive")
            return null
        }

        // Find the most recently resumed app that hasn't been paused since
        val activeApp = appStates.entries
            .filter { (pkg, state) ->
                state.isResumed && !isIgnoredPackage(pkg)
            }
            .maxByOrNull { it.value.timestamp }
            ?.key

        if (activeApp != null) {
            // Ghost activity check: if switching to a different app, verify the
            // current app was actually paused first. A real app switch always
            // pauses the current activity. If it wasn't paused, the "new" app
            // is likely a ghost background activity (e.g., transparent activity
            // for push notifications, content providers, etc.)
            if (currentApp != null && activeApp != currentApp) {
                val currentAppState = appStates[currentApp]
                if (currentAppState == null || currentAppState.isResumed) {
                    Log.d(TAG, "Ghost activity detected: $activeApp resumed but $currentApp not paused, ignoring")
                    lastConfirmedActiveTime = currentTime
                    return currentApp
                }
            }

            Log.d(TAG, "Active app detected: $activeApp")
            lastConfirmedActiveTime = currentTime
            return activeApp
        }

        // Fallback: no apps resumed in event window, but we're tracking an app.
        // Continue tracking only if:
        // 1. The current app was NOT explicitly paused in this window
        // 2. We haven't been relying on this fallback for too long (max 30 min)
        if (currentApp != null && !isIgnoredPackage(currentApp!!)) {
            val currentAppState = appStates[currentApp]

            // If current app was explicitly paused, it's no longer in foreground
            if (currentAppState != null && !currentAppState.isResumed) {
                Log.d(TAG, "Current app $currentApp was paused")
                return null
            }

            // Check fallback timeout: don't track indefinitely without event confirmation
            val timeSinceConfirmed = currentTime - lastConfirmedActiveTime
            if (lastConfirmedActiveTime > 0 && timeSinceConfirmed > Constants.MAX_TRACKING_WITHOUT_EVENTS_MS) {
                Log.d(TAG, "Fallback timeout: no events for $currentApp in ${timeSinceConfirmed / 1000}s, stopping tracking")
                return null
            }

            // App is still active - refresh confirmed time to prevent false timeout during long sessions
            lastConfirmedActiveTime = currentTime
            Log.d(TAG, "Continuing to track current app: $currentApp (no pause detected)")
            return currentApp
        }

        Log.d(TAG, "No active app detected")
        return null
    }

    /**
     * Check if a package should be ignored (launchers, system UI, our app)
     */
    private fun isIgnoredPackage(packageName: String): Boolean {
        return IGNORED_PACKAGES.any { packageName.contains(it) || it.contains(packageName) } ||
               packageName.contains("launcher") ||
               packageName.contains("systemui")
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager?.getApplicationInfo(packageName, 0)
            appInfo?.let { packageManager?.getApplicationLabel(it).toString() } ?: packageName
        } catch (e: Exception) {
            packageName
        }
    }

    fun snoozeApp(packageName: String) {
        val snoozeUntil = System.currentTimeMillis() + (Constants.SNOOZE_DURATION_MINUTES * 60 * 1000)
        snoozedApps[packageName] = snoozeUntil
    }
}
