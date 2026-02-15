package com.alertsystem.apptracker.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.alertsystem.apptracker.R
import com.alertsystem.apptracker.domain.repository.UsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UsageRepository
) {
    companion object {
        private const val TAG = "AlertOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var isShowing = false
    private var currentOverlayPackage: String? = null
    private var autoDismissRunnable: Runnable? = null

    fun canShowOverlay(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
            Log.d(TAG, "Vibration triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }

    private fun createOverlayParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
    }

    fun showAlert(
        packageName: String,
        appName: String,
        usageMinutes: Int,
        timeLimit: Int,
        addTimeIncrement: Int
    ) {
        if (!canShowOverlay()) {
            Log.w(TAG, "Cannot show overlay - permission not granted")
            return
        }

        handler.post {
            // Dismiss any existing overlay first (synchronously on main thread)
            removeOverlayView()

            // Vibrate to alert the user
            vibrate()

            try {
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(R.layout.overlay_alert, null)

                // Set text content
                view.findViewById<TextView>(R.id.tv_app_name).text = appName
                view.findViewById<TextView>(R.id.tv_usage_time).text = formatTime(usageMinutes)
                view.findViewById<TextView>(R.id.tv_time_limit).text = "Your limit: ${formatTime(timeLimit)}"

                val btnAddTime = view.findViewById<Button>(R.id.btn_add_time)
                btnAddTime.text = "Add $addTimeIncrement min"

                // Set button click handlers
                view.findViewById<Button>(R.id.btn_ignore).setOnClickListener {
                    Log.d(TAG, "User tapped Ignore for $appName")
                    handleIgnore(packageName)
                    removeOverlayView()
                }

                btnAddTime.setOnClickListener {
                    Log.d(TAG, "User tapped Add $addTimeIncrement min for $appName")
                    handleAddTime(packageName, addTimeIncrement)
                    removeOverlayView()
                }

                windowManager.addView(view, createOverlayParams())
                overlayView = view
                isShowing = true
                currentOverlayPackage = packageName

                // Auto-dismiss after 60 seconds as safety net
                autoDismissRunnable = Runnable { removeOverlayView() }
                handler.postDelayed(autoDismissRunnable!!, 60_000)

                Log.d(TAG, "Overlay shown for $appName ($usageMinutes min)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay", e)
            }
        }
    }

    fun showAddictiveWarning(packageName: String, appName: String, dailyUsageMinutes: Int) {
        if (!canShowOverlay()) {
            Log.w(TAG, "Cannot show overlay - permission not granted")
            return
        }

        handler.post {
            // Dismiss any existing overlay first (synchronously on main thread)
            removeOverlayView()

            try {
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(R.layout.overlay_addictive_warning, null)

                // Set app name
                view.findViewById<TextView>(R.id.tv_app_name).text = appName

                // Set warning message based on daily usage
                val warningMessage = if (dailyUsageMinutes < 10) {
                    "Think twice before spending time here. Do you really need to use this app right now?"
                } else {
                    "You've already spent ${formatTime(dailyUsageMinutes)} in this app today. Do you really want to continue using this app?"
                }
                view.findViewById<TextView>(R.id.tv_warning_message).text = warningMessage

                // Close App button - sends user to home screen
                view.findViewById<Button>(R.id.btn_close_app).setOnClickListener {
                    Log.d(TAG, "User tapped Close App for addictive app $appName")
                    removeOverlayView()
                    // Send user to home screen
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                }

                // Continue Anyway button - just dismiss
                view.findViewById<Button>(R.id.btn_continue).setOnClickListener {
                    Log.d(TAG, "User tapped Continue Anyway for addictive app $appName")
                    removeOverlayView()
                }

                windowManager.addView(view, createOverlayParams())
                overlayView = view
                isShowing = true
                currentOverlayPackage = packageName

                // Auto-dismiss after 60 seconds as safety net
                autoDismissRunnable = Runnable { removeOverlayView() }
                handler.postDelayed(autoDismissRunnable!!, 60_000)

                Log.d(TAG, "Addictive warning overlay shown for $appName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show addictive warning overlay", e)
            }
        }
    }

    fun dismissIfShowingForPackage(packageName: String) {
        if (isShowing && currentOverlayPackage == packageName) {
            Log.d(TAG, "Auto-dismissing overlay because user left $packageName")
            dismissAlert()
        }
    }

    fun dismissAlert() {
        handler.post { removeOverlayView() }
    }

    /**
     * Must be called on the main thread. Immediately removes the overlay view.
     * This avoids race conditions from multiple handler.post calls.
     */
    private fun removeOverlayView() {
        try {
            // Cancel any pending auto-dismiss
            autoDismissRunnable?.let { handler.removeCallbacks(it) }
            autoDismissRunnable = null

            overlayView?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                }
            }
            overlayView = null
            isShowing = false
            currentOverlayPackage = null
            Log.d(TAG, "Overlay dismissed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss overlay", e)
        }
    }

    private fun handleIgnore(packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getAppSettings(packageName)
            if (settings != null) {
                // Reset added time and increment when user ignores
                repository.updateAddedTime(packageName, 0, 5)
                // Update last notification time so it reminds again after original interval
                repository.updateLastNotificationTime(packageName, System.currentTimeMillis())
            }
        }
    }

    private fun handleAddTime(packageName: String, addMinutes: Int) {
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

    private fun formatTime(minutes: Int): String {
        return when {
            minutes >= 60 -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "${minutes}m"
        }
    }
}
