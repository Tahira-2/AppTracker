package com.alertsystem.apptracker.ui.alert

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertsystem.apptracker.ui.theme.AppTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsageAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_USAGE_MINUTES = "usage_minutes"
        const val EXTRA_TIME_LIMIT = "time_limit"
        const val EXTRA_ADD_TIME_INCREMENT = "add_time_increment"

        const val RESULT_IGNORE = "ignore"
        const val RESULT_ADD_TIME = "add_time"

        fun createIntent(
            context: Context,
            packageName: String,
            appName: String,
            usageMinutes: Int,
            timeLimit: Int,
            addTimeIncrement: Int
        ): Intent {
            return Intent(context, UsageAlertActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_USAGE_MINUTES, usageMinutes)
                putExtra(EXTRA_TIME_LIMIT, timeLimit)
                putExtra(EXTRA_ADD_TIME_INCREMENT, addTimeIncrement)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
            }
        }
    }

    private lateinit var packageNameArg: String
    private lateinit var appName: String
    private var usageMinutes: Int = 0
    private var timeLimit: Int = 0
    private var addTimeIncrement: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the activity show over lock screen and turn screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        packageNameArg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        usageMinutes = intent.getIntExtra(EXTRA_USAGE_MINUTES, 0)
        timeLimit = intent.getIntExtra(EXTRA_TIME_LIMIT, 15)
        addTimeIncrement = intent.getIntExtra(EXTRA_ADD_TIME_INCREMENT, 5)

        setContent {
            AppTrackerTheme {
                UsageAlertScreen(
                    appName = appName,
                    usageMinutes = usageMinutes,
                    timeLimit = timeLimit,
                    addTimeIncrement = addTimeIncrement,
                    onIgnore = { handleIgnore() },
                    onAddTime = { handleAddTime() }
                )
            }
        }
    }

    private fun handleIgnore() {
        // Send broadcast to service
        val intent = Intent("com.alertsystem.apptracker.ALERT_RESPONSE").apply {
            putExtra("action", RESULT_IGNORE)
            putExtra("package_name", packageNameArg)
            setPackage(this@UsageAlertActivity.packageName)
        }
        sendBroadcast(intent)
        finish()
    }

    private fun handleAddTime() {
        // Send broadcast to service
        val intent = Intent("com.alertsystem.apptracker.ALERT_RESPONSE").apply {
            putExtra("action", RESULT_ADD_TIME)
            putExtra("package_name", packageNameArg)
            putExtra("add_minutes", addTimeIncrement)
            setPackage(this@UsageAlertActivity.packageName)
        }
        sendBroadcast(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back button from dismissing the alert
        // User must select an option
    }
}

@Composable
fun UsageAlertScreen(
    appName: String,
    usageMinutes: Int,
    timeLimit: Int,
    addTimeIncrement: Int,
    onIgnore: () -> Unit,
    onAddTime: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF6B6B)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Time Limit Reached",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // App name
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Usage info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You've been using this app for",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatTime(usageMinutes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your limit: ${formatTime(timeLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = "Take a break! Your eyes and mind will thank you.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ignore button
                    OutlinedButton(
                        onClick = onIgnore,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ignore")
                    }

                    // Add Time button
                    Button(
                        onClick = onAddTime,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Add $addTimeIncrement min")
                    }
                }
            }
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
