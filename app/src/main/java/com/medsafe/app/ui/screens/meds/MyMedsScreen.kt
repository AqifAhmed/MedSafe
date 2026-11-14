@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medsafe.app.ui.screens.meds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsafe.app.data.local.entity.MedicationEntity
import com.medsafe.app.data.local.entity.MedicationWithSchedules
import com.medsafe.app.ui.components.ClayCard
import com.medsafe.app.ui.components.EmptyState
import com.medsafe.app.ui.components.PillIcon
import com.medsafe.app.ui.components.SectionHeader
import com.medsafe.app.util.Format
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMedsScreen(
    onAddMedication: () -> Unit,
    onEditMedication: (Long) -> Unit,
    viewModel: MedsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var medPendingDelete by remember { mutableStateOf<MedicationEntity?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("My Meds", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = state.profile?.let { "${it.name}'s medications" } ?: "Medications",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
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
            if (state.active.isEmpty() && state.archived.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Medication,
                        title = "No medications yet",
                        message = "Add the first medication for this profile. You can set up to six daily times.",
                        actionLabel = "Add medication",
                        onAction = onAddMedication,
                    )
                }
            }

            if (state.active.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Active",
                        subtitle = "${state.active.size} medication${if (state.active.size == 1) "" else "s"}",
                    )
                }
                items(state.active, key = { it.medication.id }) { item ->
                    MedicationCard(
                        item = item,
                        onEdit = { onEditMedication(item.medication.id) },
                        onArchive = { viewModel.setArchived(item.medication.id, true) },
                        onDelete = { medPendingDelete = item.medication },
                    )
                }
            }

            if (state.archived.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showArchived = !showArchived },
                        color = Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionHeader(
                                title = "Archived",
                                subtitle = "${state.archived.size} medication${if (state.archived.size == 1) "" else "s"}",
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                if (showArchived) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (showArchived) "Collapse archived" else "Expand archived",
                            )
                        }
                    }
                }
                if (showArchived) {
                    items(state.archived, key = { it.medication.id }) { item ->
                        MedicationCard(
                            item = item,
                            onEdit = { onEditMedication(item.medication.id) },
                            onArchive = { viewModel.setArchived(item.medication.id, false) },
                            onDelete = { medPendingDelete = item.medication },
                        )
                    }
                }
            }
        }
    }

    medPendingDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = { medPendingDelete = null },
            title = { Text("Delete ${medication.name}?") },
            text = {
                Text("This also deletes its schedule and dose history. Archiving keeps the history if you might need it later.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(medication.id)
                        medPendingDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { medPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MedicationCard(
    item: MedicationWithSchedules,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val medication = item.medication
    var menuOpen by remember { mutableStateOf(false) }
    val lowStock = medication.stock != null &&
        medication.lowStockThreshold != null &&
        medication.stock <= medication.lowStockThreshold

    ClayCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
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
                    Text(medication.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${Format.doseAmount(medication)} · ${medication.form.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Medication options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (medication.archived) "Restore" else "Archive") },
                            leadingIcon = {
                                Icon(
                                    if (medication.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onArchive()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DurationBadge(medication)
                Text(
                    text = Format.scheduleSummary(item.schedules),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (lowStock) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Low stock: ${medication.stock} left",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (!medication.isIndefinite && medication.endDate != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Ends ${Format.date(medication.endDate)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DurationBadge(medication: MedicationEntity) {
    val label = Format.durationBadge(medication, LocalDate.now())
    val container = when {
        medication.archived -> MaterialTheme.colorScheme.surfaceContainerHighest
        medication.isIndefinite -> MaterialTheme.colorScheme.secondaryContainer
        (Format.daysLeft(medication) ?: 1) < 0 -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when {
        medication.archived -> MaterialTheme.colorScheme.onSurfaceVariant
        medication.isIndefinite -> MaterialTheme.colorScheme.onSecondaryContainer
        (Format.daysLeft(medication) ?: 1) < 0 -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
