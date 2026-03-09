package com.alertsystem.apptracker.ui.screens.settings

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertsystem.apptracker.ui.theme.AddictiveRed
import com.alertsystem.apptracker.ui.theme.SuccessGreen
import com.alertsystem.apptracker.ui.theme.WarningOrange
import com.alertsystem.apptracker.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
                actions = {
                    IconButton(onClick = { viewModel.refreshApps() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search apps...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Enable notifications for apps you want reminders for. Usage is always tracked in the background.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(viewModel.getFilteredApps()) { app ->
                            AppSettingsCard(
                                app = app,
                                onTimeLimitChange = { minutes ->
                                    viewModel.updateTimeLimit(app.packageName, minutes)
                                },
                                onExclusionToggle = {
                                    viewModel.onExclusionToggleClicked(app)
                                },
                                onNotificationToggle = { enabled ->
                                    viewModel.onNotificationToggle(app, enabled)
                                },
                                onAddictiveToggle = { isAddictive ->
                                    viewModel.toggleAddictive(app, isAddictive)
                                },
                                onWorkToggle = { isWork ->
                                    viewModel.toggleWork(app.packageName, isWork)
                                }
                            )
                        }
                    }
                }
            }

            // Exclusion confirmation dialog
            if (uiState.showExclusionDialog && uiState.selectedApp != null) {
                ExclusionConfirmationDialog(
                    app = uiState.selectedApp!!,
                    confirmationStep = uiState.confirmationStep,
                    onConfirm = { viewModel.confirmExclusion() },
                    onDismiss = { viewModel.dismissExclusionDialog() }
                )
            }

            // Unsubscribe confirmation dialog for addictive apps
            if (uiState.showUnsubscribeDialog && uiState.pendingUnsubscribeApp != null) {
                val app = uiState.pendingUnsubscribeApp!!

                AlertDialog(
                    onDismissRequest = { viewModel.dismissUnsubscribeDialog() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AddictiveRed,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Disable Reminders?",
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Text(
                            text = "${app.appName} is marked as addictive. Are you sure you want to disable reminders for this app?",
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmUnsubscribeAddictive() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AddictiveRed
                            )
                        ) {
                            Text("Disable Reminders")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUnsubscribeDialog() }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Unmark addictive warning dialog
            if (uiState.showUnmarkAddictiveDialog && uiState.pendingUnmarkApp != null) {
                val app = uiState.pendingUnmarkApp!!
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUnmarkAddictiveDialog() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Remove Addictive Label?",
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${app.appName} is currently marked as addictive.",
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Removing this label will disable extra safeguards like confirmation prompts for disabling notifications. Are you sure?",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmUnmarkAddictive() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarningOrange
                            )
                        ) {
                            Text("Yes, Remove")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUnmarkAddictiveDialog() }) {
                            Text("Keep Label")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppSettingsCard(
    app: AppSettingsItem,
    onTimeLimitChange: (Int) -> Unit,
    onExclusionToggle: () -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onAddictiveToggle: (Boolean) -> Unit,
    onWorkToggle: (Boolean) -> Unit
) {
    var sliderValue by remember(app.timeLimitMinutes) {
        mutableFloatStateOf(app.timeLimitMinutes.toFloat())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = when {
            app.isAddictive -> CardDefaults.cardColors(
                containerColor = AddictiveRed.copy(alpha = 0.1f)
            )
            app.isWork -> CardDefaults.cardColors(
                containerColor = SuccessGreen.copy(alpha = 0.08f)
            )
            else -> CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // App header with notification toggle
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (app.isAddictive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Addictive",
                                modifier = Modifier.size(16.dp),
                                tint = AddictiveRed
                            )
                        }
                    }
                    if (app.isAddictive) {
                        Text(
                            text = "Addictive",
                            style = MaterialTheme.typography.bodySmall,
                            color = AddictiveRed
                        )
                    }
                    if (app.isWork) {
                        Text(
                            text = "Work",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen
                        )
                    }
                }

                // Notification toggle (main toggle)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "Notify",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Switch(
                        checked = app.isNotificationEnabled,
                        onCheckedChange = { onNotificationToggle(it) },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // Show time limit and addictive toggle only if notifications are enabled
            if (app.isNotificationEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // Time limit slider
                Text(
                    text = "Reminder after: ${sliderValue.toInt()} minutes",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        onTimeLimitChange(sliderValue.toInt())
                    },
                    valueRange = Constants.MIN_TIME_LIMIT_MINUTES.toFloat()..Constants.MAX_TIME_LIMIT_MINUTES.toFloat(),
                    steps = (Constants.MAX_TIME_LIMIT_MINUTES - Constants.MIN_TIME_LIMIT_MINUTES) / 5 - 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${Constants.MIN_TIME_LIMIT_MINUTES}m",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${Constants.MAX_TIME_LIMIT_MINUTES}m",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Work app toggle
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Work app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = app.isWork,
                    onCheckedChange = { onWorkToggle(it) },
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SuccessGreen,
                        checkedTrackColor = SuccessGreen.copy(alpha = 0.5f)
                    )
                )
            }

            // Manual addictive toggle (hidden if Work app)
            if (!app.isWork) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mark as addictive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = app.isAddictive,
                        onCheckedChange = { onAddictiveToggle(it) },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AddictiveRed,
                            checkedTrackColor = AddictiveRed.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ExclusionConfirmationDialog(
    app: AppSettingsItem,
    confirmationStep: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val requiredConfirmations = if (app.isAddictive) {
        Constants.ADDICTIVE_APP_CONFIRM_COUNT
    } else {
        Constants.NORMAL_APP_CONFIRM_COUNT
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            if (app.isAddictive) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AddictiveRed,
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        title = {
            Text(
                text = if (app.isAddictive) {
                    "Exclude Addictive App?"
                } else {
                    "Disable Reminders?"
                },
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                if (app.isAddictive) {
                    Text(
                        text = "This app is considered addictive based on your usage patterns.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AddictiveRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Disabling reminders may lead to increased screen time. Are you sure you want to disable reminders for ${app.appName}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Confirmation ${confirmationStep} of $requiredConfirmations",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "Are you sure you want to disable reminders for ${app.appName}? Usage will still be tracked in the background.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (app.isAddictive) {
                    ButtonDefaults.buttonColors(
                        containerColor = AddictiveRed
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    text = if (confirmationStep < requiredConfirmations) {
                        "Confirm (${confirmationStep}/${requiredConfirmations})"
                    } else {
                        "Yes, Exclude"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
