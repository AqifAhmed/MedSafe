package com.medsafe.app.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medsafe.app.data.model.PillShape
import com.medsafe.app.ui.components.PillIcon
import com.medsafe.app.ui.theme.ClayPrimary
import com.medsafe.app.ui.theme.ClaySecondary
import com.medsafe.app.ui.util.rememberResumeCounter
import com.medsafe.app.util.Permissions

private const val STEP_WELCOME = 0
private const val STEP_NOTIFICATIONS = 1
private const val STEP_EXACT = 2
private const val STEP_BATTERY = 3
private const val STEP_DONE = 4
private const val STEP_COUNT = 5

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var step by rememberSaveable { mutableStateOf(STEP_WELCOME) }
    val context = LocalContext.current
    val resumeCounter = rememberResumeCounter()

    val canNotify = remember(resumeCounter) { Permissions.canPostNotifications(context) }
    val canExact = remember(resumeCounter) { Permissions.canScheduleExactAlarms(context) }
    val batteryExempt = remember(resumeCounter) { Permissions.isIgnoringBatteryOptimizations(context) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillIcon(
                shape = PillShape.CAPSULE,
                color1 = ClayPrimary,
                color2 = ClaySecondary,
                size = 32.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text("MedSafe", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            if (step in 1..STEP_BATTERY) {
                TextButton(onClick = { step -= 1 }) { Text("Back") }
            }
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { (step + 1).toFloat() / STEP_COUNT },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (step) {
                STEP_WELCOME -> WelcomeStep()
                STEP_NOTIFICATIONS -> PermissionStep(
                    icon = Icons.Rounded.Notifications,
                    title = "Dose notifications",
                    headline = if (canNotify) "Notifications are on" else "Allow notifications",
                    body = "MedSafe sends a notification at every scheduled dose, with Taken, " +
                        "Snooze and Skip actions that work without opening the app. On Android 13 " +
                        "and later, notifications must be allowed explicitly.",
                    granted = canNotify,
                )
                STEP_EXACT -> PermissionStep(
                    icon = Icons.Rounded.Alarm,
                    title = "Exact timing",
                    headline = if (canExact) "Exact reminders are on" else "Allow alarms & reminders",
                    body = "This lets Android deliver each notification at the exact minute you set. " +
                        "MedSafe does not ring an alarm; it only posts a normal notification. " +
                        "Without it, reminders can arrive a few minutes late.",
                    granted = canExact,
                )
                STEP_BATTERY -> PermissionStep(
                    icon = Icons.Rounded.BatteryAlert,
                    title = "Battery optimization",
                    headline = if (batteryExempt) "MedSafe is exempt" else "Keep reminders reliable",
                    body = "Aggressive battery management on Xiaomi, Oppo, Vivo, Samsung and Huawei " +
                        "phones can stop reminders while the app is in the background. Exempt " +
                        "MedSafe from battery optimization, and check the manufacturer's app " +
                        "settings for an extra “Autostart” or “No restrictions” switch. Avoid force-stopping " +
                        "MedSafe: Android cancels its reminders until you open the app again.",
                    granted = batteryExempt,
                )
                STEP_DONE -> DoneStep()
            }
        }

        when (step) {
            STEP_WELCOME -> {
                PrimaryButton(text = "Get started", onClick = { step = STEP_NOTIFICATIONS })
            }
            STEP_NOTIFICATIONS -> {
                PrimaryButton(
                    text = if (canNotify) "Continue" else "Allow notifications",
                    onClick = {
                        if (canNotify) {
                            step = STEP_EXACT
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            Permissions.openNotificationSettings(context)
                        }
                    },
                )
                TextButton(
                    onClick = { step = STEP_EXACT },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) { Text("Skip for now") }
            }
            STEP_EXACT -> {
                PrimaryButton(
                    text = if (canExact) "Continue" else "Open alarm settings",
                    onClick = {
                        if (canExact) {
                            step = STEP_BATTERY
                        } else {
                            Permissions.openExactAlarmSettings(context)
                        }
                    },
                )
                TextButton(
                    onClick = { step = STEP_BATTERY },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) { Text("Skip for now") }
            }
            STEP_BATTERY -> {
                PrimaryButton(
                    text = if (batteryExempt) "Continue" else "Open battery settings",
                    onClick = {
                        if (batteryExempt) {
                            step = STEP_DONE
                        } else {
                            Permissions.openBatteryOptimizationSettings(context)
                        }
                    },
                )
                TextButton(
                    onClick = { step = STEP_DONE },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) { Text("Skip for now") }
            }
            STEP_DONE -> {
                PrimaryButton(
                    text = "Start using MedSafe",
                    onClick = { viewModel.complete(onFinished) },
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(24.dp))
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        PillIcon(
            shape = PillShape.HALF_CAPSULE,
            color1 = ClayPrimary,
            color2 = ClaySecondary,
            size = 120.dp,
            modifier = Modifier.padding(20.dp),
        )
    }
    Spacer(Modifier.height(28.dp))
    Text(
        text = "Welcome to MedSafe",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Track medications and get a reminder at every dose — for yourself and up to " +
            "two family members. Everything stays on this phone: no account and no cloud.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PermissionStep(
    icon: ImageVector,
    title: String,
    headline: String,
    body: String,
    granted: Boolean,
) {
    Spacer(Modifier.height(16.dp))
    Surface(
        shape = CircleShape,
        color = if (granted) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (granted) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .padding(20.dp)
                .size(40.dp),
        )
    }
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Text(
        text = headline,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    if (granted) {
        Spacer(Modifier.height(20.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Text(
                "Ready",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun DoneStep() {
    Spacer(Modifier.height(24.dp))
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        PillIcon(
            shape = PillShape.CAPSULE,
            color1 = ClayPrimary,
            color2 = ClaySecondary,
            size = 120.dp,
            modifier = Modifier.padding(20.dp),
        )
    }
    Spacer(Modifier.height(28.dp))
    Text(
        text = "You're all set",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Start by adding a medication for the active profile. You can create or switch " +
            "profiles at any time from the Today screen.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
