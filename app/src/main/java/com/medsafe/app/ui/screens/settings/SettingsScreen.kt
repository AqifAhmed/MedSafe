@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medsafe.app.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsafe.app.data.settings.ThemeMode
import com.medsafe.app.ui.components.ClayCard
import com.medsafe.app.ui.components.SectionHeader
import com.medsafe.app.ui.util.rememberResumeCounter
import com.medsafe.app.util.Permissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenProfiles: () -> Unit,
    onOpenOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resumeCounter = rememberResumeCounter()

    val canNotify = remember(resumeCounter) { Permissions.canPostNotifications(context) }
    val canExactAlarm = remember(resumeCounter) { Permissions.canScheduleExactAlarms(context) }
    val batteryExempt = remember(resumeCounter) { Permissions.isIgnoringBatteryOptimizations(context) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsCard {
                    SectionHeader(title = "Appearance")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip(
                            label = "System",
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setTheme(ThemeMode.SYSTEM) },
                        )
                        ThemeChip(
                            label = "Light",
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setTheme(ThemeMode.LIGHT) },
                        )
                        ThemeChip(
                            label = "Dark",
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setTheme(ThemeMode.DARK) },
                        )
                    }
                }
            }

            item {
                SettingsCard {
                    SectionHeader(
                        title = "Reminders",
                        subtitle = "Used by every profile on this device.",
                    )
                    Spacer(Modifier.height(6.dp))
                    StepperRow(
                        title = "Snooze length",
                        description = "How long Snooze postpones a reminder.",
                        value = "${settings.snoozeMinutes} min",
                        onDecrease = { viewModel.setSnoozeMinutes(settings.snoozeMinutes - 5) },
                        onIncrease = { viewModel.setSnoozeMinutes(settings.snoozeMinutes + 5) },
                    )
                    StepperRow(
                        title = "Re-nudge after",
                        description = "If a dose is not answered, nudge once, then mark it missed.",
                        value = "${settings.renudgeMinutes} min",
                        onDecrease = { viewModel.setRenudgeMinutes(settings.renudgeMinutes - 5) },
                        onIncrease = { viewModel.setRenudgeMinutes(settings.renudgeMinutes + 5) },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Notification sound", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "The alert tone for dose reminders.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.soundEnabled,
                            onCheckedChange = viewModel::setSoundEnabled,
                        )
                    }
                }
            }

            item {
                SettingsCard {
                    SectionHeader(
                        title = "Permissions",
                        subtitle = "Reminder apps need these to work reliably.",
                    )
                    Spacer(Modifier.height(8.dp))
                    PermissionRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        description = if (canNotify) "Allowed" else "Off — reminders cannot appear.",
                        granted = canNotify,
                        actionLabel = if (canNotify) "Manage" else "Allow",
                        onAction = {
                            if (canNotify) {
                                Permissions.openNotificationSettings(context)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Permissions.openNotificationSettings(context)
                            }
                        },
                    )
                    PermissionRow(
                        icon = Icons.Rounded.Schedule,
                        title = "Exact alarms",
                        description = when {
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> "Not required on this Android version."
                            canExactAlarm -> "Allowed. Reminders fire at the exact time."
                            else -> "Off — reminders may arrive late."
                        },
                        granted = canExactAlarm,
                        actionLabel = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) null else "Open",
                        onAction = { Permissions.openExactAlarmSettings(context) },
                    )
                    PermissionRow(
                        icon = Icons.Rounded.BatteryAlert,
                        title = "Battery optimization",
                        description = if (batteryExempt) {
                            "Exempt — the system will not delay reminders."
                        } else {
                            "Not exempt — some phones delay reminders in the background."
                        },
                        granted = batteryExempt,
                        actionLabel = "Open",
                        onAction = { Permissions.openBatteryOptimizationSettings(context) },
                    )
                }
            }

            item {
                SettingsCard {
                    SectionHeader(
                        title = "Battery-optimization help",
                        subtitle = "The most common cause of missed reminders",
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "On Xiaomi, Oppo, Vivo, Samsung and Huawei devices, also set MedSafe " +
                            "to “No restrictions” or “Autostart allowed” in the phone's battery settings. " +
                            "The button above opens the Android exemption screen; the manufacturer's " +
                            "app settings may have an extra switch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { Permissions.openAppDetailsSettings(context) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Open app settings")
                    }
                }
            }

            item {
                SettingsCard {
                    SectionHeader(title = "Profiles")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onOpenProfiles,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Rounded.People, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manage profiles")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }

            item {
                SettingsCard {
                    SectionHeader(title = "Setup guide")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Re-run the permission and battery-optimization guide at any time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenOnboarding,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open setup guide")
                    }
                }
            }

            item {
                SettingsCard {
                    SectionHeader(title = "About")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "MedSafe 1.0.0\nLocal-first: your data stays on this device. " +
                            "No account, no cloud sync, no medication strength tracking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun StepperRow(
    title: String,
    description: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDecrease) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease $title")
        }
        Text(value, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onIncrease) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase $title")
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            contentDescription = if (granted) "Granted" else "Not granted",
            tint = if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        )
        if (actionLabel != null) {
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
