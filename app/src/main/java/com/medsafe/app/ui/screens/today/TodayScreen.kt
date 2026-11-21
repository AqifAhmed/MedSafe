@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medsafe.app.ui.screens.today

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsafe.app.data.local.entity.ProfileEntity
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.ui.components.ClayCard
import com.medsafe.app.ui.components.DoseStatusBadge
import com.medsafe.app.ui.components.EmptyState
import com.medsafe.app.ui.components.InfoBanner
import com.medsafe.app.ui.components.PillIcon
import com.medsafe.app.ui.components.ProfileAvatar
import com.medsafe.app.ui.components.SectionHeader
import com.medsafe.app.ui.util.rememberResumeCounter
import com.medsafe.app.util.Format
import com.medsafe.app.util.Permissions
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddMedication: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resumeCounter = rememberResumeCounter()
    LaunchedEffect(resumeCounter) { viewModel.refresh() }

    val canNotify = remember(resumeCounter) { Permissions.canPostNotifications(context) }
    val canExact = remember(resumeCounter) { Permissions.canScheduleExactAlarms(context) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    val groups = state.doses
        .groupBy { timeBucket(it.time) }
        .toList()
        .sortedBy { it.first.order }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    windowInsets = WindowInsets(0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    title = {
                        Column {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = state.today.format(
                                    DateTimeFormatter.ofPattern(
                                        "EEEE, d MMMM",
                                        Locale.getDefault(),
                                    ),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenProfiles) {
                            Icon(Icons.Rounded.People, contentDescription = "Profiles")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    },
                )
                ProfileChipRow(
                    profiles = state.profiles,
                    activeProfileId = state.profile?.id,
                    onSelect = viewModel::switchProfile,
                    onManage = onOpenProfiles,
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMedication,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add medication") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!canNotify) {
                item {
                    InfoBanner(
                        title = "Notifications are off",
                        message = "Dose reminders cannot appear until notifications are allowed.",
                        icon = Icons.Rounded.NotificationsOff,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        actionLabel = "Allow",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Permissions.openNotificationSettings(context)
                            }
                        },
                    )
                }
            }

            if (!canExact) {
                item {
                    InfoBanner(
                        title = "Exact timing is off",
                        message = "Reminders may arrive a few minutes late until you allow alarms & reminders.",
                        icon = Icons.Rounded.AlarmOff,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        actionLabel = "Allow",
                        onAction = { Permissions.openExactAlarmSettings(context) },
                    )
                }
            }

            if (state.doses.isNotEmpty()) {
                item { TodaySummaryCard(state.doses) }
            }

            if (state.doses.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Medication,
                        title = "No doses today",
                        message = if (state.profile != null) {
                            "Add a medication for ${state.profile!!.name} to see today's schedule here."
                        } else {
                            "Add a medication to start tracking doses."
                        },
                        actionLabel = "Add medication",
                        onAction = onAddMedication,
                    )
                }
            }

            groups.forEach { (bucket, doses) ->
                item(key = "section-${bucket.order}") {
                    SectionHeader(
                        title = bucket.label,
                        subtitle = "${doses.count { it.status == DoseStatus.TAKEN }} of ${doses.size} taken",
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                items(
                    items = doses,
                    key = { "${it.medication.id}-${it.scheduledAt}" },
                ) { dose ->
                    DoseCard(
                        dose = dose,
                        onTaken = { viewModel.markTaken(dose) },
                        onSnooze = { viewModel.snooze(dose) },
                        onSkip = { viewModel.skip(dose) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileChipRow(
    profiles: List<ProfileEntity>,
    activeProfileId: Long?,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
) {
    if (profiles.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        profiles.forEach { profile ->
            FilterChip(
                selected = profile.id == activeProfileId,
                onClick = { onSelect(profile.id) },
                label = { Text(profile.name) },
                leadingIcon = { ProfileAvatar(profile = profile, size = 22.dp) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        TextButton(onClick = onManage) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (profiles.size >= 3) "Profiles" else "Add profile")
        }
    }
}

@Composable
private fun TodaySummaryCard(doses: List<TodayDose>) {
    val taken = doses.count { it.status == DoseStatus.TAKEN }
    val total = doses.size
    val progress = if (total == 0) 0f else taken.toFloat() / total
    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$taken of $total doses taken",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (taken == total) "All done" else "${total - taken} left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun DoseCard(
    dose: TodayDose,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
) {
    val medication = dose.medication
    val canAct = dose.status == DoseStatus.PENDING ||
        dose.status == DoseStatus.SNOOZED ||
        dose.status == DoseStatus.MISSED

    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillIcon(
                    shape = medication.pillShape,
                    color1 = Color(medication.pillColor1),
                    color2 = medication.pillColor2?.let { Color(it) },
                    size = 48.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Format.time(dose.time),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.weight(1f))
                        DoseStatusBadge(dose.status)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = buildString {
                            append(Format.doseAmount(medication))
                            medication.notes?.takeIf { it.isNotBlank() }?.let {
                                append(" · ").append(it)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (canAct) {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onTaken,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (dose.status == DoseStatus.MISSED) "Taken late" else "Taken")
                    }
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Snooze")
                    }
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}

private data class TimeBucket(val order: Int, val label: String)

private fun timeBucket(time: LocalTime): TimeBucket = when (time.hour) {
    in 0..11 -> TimeBucket(0, "Morning")
    in 12..16 -> TimeBucket(1, "Afternoon")
    in 17..20 -> TimeBucket(2, "Evening")
    else -> TimeBucket(3, "Night")
}
