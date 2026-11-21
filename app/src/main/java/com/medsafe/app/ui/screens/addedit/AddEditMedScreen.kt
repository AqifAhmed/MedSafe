@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medsafe.app.ui.screens.addedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsafe.app.data.model.DurationMode
import com.medsafe.app.data.model.MedicationForm
import com.medsafe.app.data.model.PillShape
import com.medsafe.app.data.model.RepeatMode
import com.medsafe.app.ui.components.ClayCard
import com.medsafe.app.ui.components.PillIcon
import com.medsafe.app.ui.components.SectionHeader
import com.medsafe.app.ui.components.onColorFor
import com.medsafe.app.ui.theme.PillPalette
import com.medsafe.app.ui.util.showDatePicker
import com.medsafe.app.ui.util.showTimePicker
import com.medsafe.app.util.Format
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedScreen(
    onBack: () -> Unit,
    viewModel: AddEditMedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var colorTarget by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.step) { scrollState.scrollTo(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (state.isEditing) "Edit medication" else "Add medication",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        if (state.profileName.isNotBlank()) {
                            Text(
                                text = "For ${state.profileName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.step > 0) {
                            OutlinedButton(
                                onClick = viewModel::back,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp),
                                enabled = !state.saving,
                            ) { Text("Back") }
                        }
                        Button(
                            onClick = {
                                if (state.step == AddEditMedViewModel.LAST_STEP) {
                                    viewModel.save(onSaved = onBack)
                                } else {
                                    viewModel.next()
                                }
                            },
                            modifier = Modifier
                                .weight(if (state.step > 0) 2f else 1f)
                                .heightIn(min = 52.dp),
                            enabled = !state.saving,
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(
                                    if (state.step == AddEditMedViewModel.LAST_STEP) {
                                        "Save medication"
                                    } else {
                                        "Next"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                StepHeader(step = state.step)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (state.step) {
                        AddEditMedViewModel.STEP_BASICS -> BasicsStep(state, viewModel)
                        AddEditMedViewModel.STEP_PILL -> PillStep(
                            state = state,
                            viewModel = viewModel,
                            onPickCustomColor = { target -> colorTarget = target },
                        )
                        AddEditMedViewModel.STEP_SCHEDULE -> ScheduleStep(
                            state = state,
                            viewModel = viewModel,
                            onPickTime = { index ->
                                showTimePicker(context, state.times[index]) { picked ->
                                    viewModel.updateTime(index, picked)
                                }
                            },
                        )
                        AddEditMedViewModel.STEP_DURATION -> DurationStep(
                            state = state,
                            viewModel = viewModel,
                            onPickStartDate = {
                                showDatePicker(context, state.startDate) { picked ->
                                    viewModel.update { it.copy(startDate = picked) }
                                }
                            },
                            onPickEndDate = {
                                showDatePicker(context, state.endDate) { picked ->
                                    viewModel.update { it.copy(endDate = picked) }
                                }
                            },
                        )
                        AddEditMedViewModel.STEP_EXTRAS -> ExtrasStep(state, viewModel)
                    }
                }
            }
        }
    }

    if (colorTarget != 0) {
        val initial = if (colorTarget == 1) state.pillColor1 else state.pillColor2
        ColorPickerDialog(
            initial = initial,
            onDismiss = { colorTarget = 0 },
            onPick = { picked ->
                viewModel.update {
                    if (colorTarget == 1) {
                        it.copy(pillColor1 = picked)
                    } else {
                        it.copy(pillColor2 = picked, twoColors = true)
                    }
                }
                colorTarget = 0
            },
        )
    }
}

@Composable
private fun StepHeader(step: Int) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Step ${step + 1} of ${AddEditMedViewModel.STEP_COUNT} · " +
                AddEditMedViewModel.STEP_TITLES[step],
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { (step + 1).toFloat() / AddEditMedViewModel.STEP_COUNT },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun BasicsStep(state: MedFormState, viewModel: AddEditMedViewModel) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(title = "What is it?")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = { value -> viewModel.update { it.copy(name = value.take(60)) } },
                label = { Text("Medication name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Text("Form", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MedicationForm.entries) { form ->
                    FilterChip(
                        selected = state.form == form,
                        onClick = { viewModel.setForm(form) },
                        label = { Text(form.label) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.doseQty,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("\\d{0,3}(\\.\\d{0,2})?"))) {
                            viewModel.update { it.copy(doseQty = value) }
                        }
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.doseUnit,
                    onValueChange = { value ->
                        viewModel.update { it.copy(doseUnit = value.take(20)) }
                    },
                    label = { Text("Unit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Dose is a quantity only — MedSafe does not track strength (mg).",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = { value -> viewModel.update { it.copy(notes = value.take(300)) } },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. take with food") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PillStep(
    state: MedFormState,
    viewModel: AddEditMedViewModel,
    onPickCustomColor: (Int) -> Unit,
) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PillIcon(
                shape = state.pillShape,
                color1 = Color(state.pillColor1),
                color2 = if (state.twoColors) Color(state.pillColor2) else null,
                size = 104.dp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.name.ifBlank { "Your medication" },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }

    SectionHeader(title = "Shape", subtitle = "Drawn in-app, no image assets required.")
    ShapeGrid(
        selected = state.pillShape,
        color1 = state.pillColor1,
        color2 = if (state.twoColors) state.pillColor2 else null,
        onSelect = { shape -> viewModel.update { it.copy(pillShape = shape) } },
    )

    SectionHeader(title = "Colours")
    Text(
        text = "First colour",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PaletteRow(
        selected = state.pillColor1,
        onSelect = { color -> viewModel.update { it.copy(pillColor1 = color) } },
        onCustom = { onPickCustomColor(1) },
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Two colours", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Used by half & half capsules and by the liquid in injections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = state.twoColors,
            onCheckedChange = { checked ->
                viewModel.update { it.copy(twoColors = checked) }
            },
        )
    }
    if (state.twoColors) {
        Text(
            text = "Second colour",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PaletteRow(
            selected = state.pillColor2,
            onSelect = { color -> viewModel.update { it.copy(pillColor2 = color) } },
            onCustom = { onPickCustomColor(2) },
        )
    }
}

@Composable
private fun ShapeGrid(
    selected: PillShape,
    color1: Int,
    color2: Int?,
    onSelect: (PillShape) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PillShape.entries.chunked(4).forEach { rowShapes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowShapes.forEach { shape ->
                    val isSelected = shape == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.medium,
                            )
                            .clickable { onSelect(shape) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PillIcon(
                            shape = shape,
                            color1 = Color(color1),
                            color2 = color2?.let { Color(it) },
                            size = 34.dp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = shape.label,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
                repeat(4 - rowShapes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PaletteRow(
    selected: Int,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillPalette.forEach { color ->
            ColorSwatch(
                color = color,
                selected = selected == color,
                onClick = { onSelect(color) },
            )
        }
        OutlinedButton(
            onClick = onCustom,
            modifier = Modifier.heightIn(min = 44.dp),
        ) { Text("Custom…") }
    }
}

@Composable
private fun ColorSwatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Selected colour",
                tint = onColorFor(color),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ScheduleStep(
    state: MedFormState,
    viewModel: AddEditMedViewModel,
    onPickTime: (Int) -> Unit,
) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(
                title = "Times of day",
                subtitle = "Up to ${AddEditMedViewModel.MAX_TIMES} daily times. " +
                    "Reminders arrive as one morning and one evening summary.",
            )
            Spacer(Modifier.height(10.dp))
            state.times.forEachIndexed { index, time ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onPickTime(index) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = Format.time(time),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { viewModel.removeTime(index) },
                        enabled = state.times.size > 1,
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remove time")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = viewModel::addTime,
                enabled = state.times.size < AddEditMedViewModel.MAX_TIMES,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add another time")
            }
        }
    }

    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(
                title = "Repeat",
                subtitle = "Applies to all times above.",
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.repeatMode == RepeatMode.DAILY,
                    onClick = { viewModel.update { it.copy(repeatMode = RepeatMode.DAILY) } },
                    label = { Text("Every day") },
                )
                FilterChip(
                    selected = state.repeatMode == RepeatMode.WEEKDAYS,
                    onClick = { viewModel.update { it.copy(repeatMode = RepeatMode.WEEKDAYS) } },
                    label = { Text("Specific days") },
                )
                FilterChip(
                    selected = state.repeatMode == RepeatMode.INTERVAL,
                    onClick = { viewModel.update { it.copy(repeatMode = RepeatMode.INTERVAL) } },
                    label = { Text("Every X days") },
                )
            }
            when (state.repeatMode) {
                RepeatMode.WEEKDAYS -> {
                    Spacer(Modifier.height(12.dp))
                    DayOfWeek.entries.chunked(4).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            rowDays.forEach { day ->
                                FilterChip(
                                    selected = day in state.weekdays,
                                    onClick = { viewModel.toggleWeekday(day) },
                                    label = { Text(Format.weekdayShort(day)) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(4 - rowDays.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                RepeatMode.INTERVAL -> {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.intervalDays,
                        onValueChange = { value ->
                            if (value.length <= 3 && value.all { it.isDigit() }) {
                                viewModel.update { it.copy(intervalDays = value) }
                            }
                        },
                        label = { Text("Repeat every N days") },
                        supportingText = { Text("Counted from the start date.") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                RepeatMode.DAILY -> Unit
            }
        }
    }
}

@Composable
private fun DurationStep(
    state: MedFormState,
    viewModel: AddEditMedViewModel,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit,
) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(
                title = "How long?",
                subtitle = "Fixed-duration medications auto-archive after the end date.",
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Start date", style = MaterialTheme.typography.titleMedium)
                    Text(
                        Format.date(state.startDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onPickStartDate,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Change") }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.durationMode == DurationMode.ONGOING,
                    onClick = { viewModel.update { it.copy(durationMode = DurationMode.ONGOING) } },
                    label = { Text("Ongoing") },
                )
                FilterChip(
                    selected = state.durationMode != DurationMode.ONGOING,
                    onClick = {
                        if (state.durationMode == DurationMode.ONGOING) {
                            viewModel.update { it.copy(durationMode = DurationMode.FIXED_DAYS) }
                        }
                    },
                    label = { Text("Fixed") },
                )
            }
            if (state.durationMode != DurationMode.ONGOING) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.durationMode == DurationMode.FIXED_DAYS,
                        onClick = {
                            viewModel.update { it.copy(durationMode = DurationMode.FIXED_DAYS) }
                        },
                        label = { Text("Number of days") },
                    )
                    FilterChip(
                        selected = state.durationMode == DurationMode.FIXED_END_DATE,
                        onClick = {
                            viewModel.update { it.copy(durationMode = DurationMode.FIXED_END_DATE) }
                        },
                        label = { Text("End date") },
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (state.durationMode == DurationMode.FIXED_DAYS) {
                    OutlinedTextField(
                        value = state.durationDays,
                        onValueChange = { value ->
                            if (value.length <= 3 && value.all { it.isDigit() }) {
                                viewModel.update { it.copy(durationDays = value) }
                            }
                        },
                        label = { Text("Days") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val days = state.durationDays.toIntOrNull() ?: 0
                    if (days > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ends ${Format.date(state.startDate.plusDays(days.toLong() - 1))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("End date", style = MaterialTheme.typography.titleMedium)
                            Text(
                                Format.date(state.endDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = onPickEndDate,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text("Change") }
                    }
                    val days = ChronoUnit.DAYS.between(state.startDate, state.endDate) + 1
                    if (days > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Lasts $days day${if (days == 1L) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = if (state.durationMode == DurationMode.ONGOING) {
                        "Ongoing medications keep reminding until you archive or delete them."
                    } else {
                        "After the end date this medication is archived automatically: it stays in " +
                            "My Meds and History, but no longer schedules reminders."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ExtrasStep(state: MedFormState, viewModel: AddEditMedViewModel) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Track stock", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Optional. MedSafe can warn you before you run out.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.hasStock,
                    onCheckedChange = { checked ->
                        viewModel.update { it.copy(hasStock = checked) }
                    },
                )
            }
            if (state.hasStock) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.stock,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() }) {
                                viewModel.update { it.copy(stock = value) }
                            }
                        },
                        label = { Text("Stock (${state.doseUnit.trim().ifBlank { "units" }})") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.lowStockThreshold,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() }) {
                                viewModel.update { it.copy(lowStockThreshold = value) }
                            }
                        },
                        label = { Text("Low at") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Stock is counted in ${state.doseUnit.trim().ifBlank { "units" }}. It drops by the dose quantity each time you mark a dose Taken and never goes below zero.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(title = "Review")
            Spacer(Modifier.height(10.dp))
            ReviewRow("Medication", state.name.ifBlank { "—" })
            ReviewRow("Dose", "${state.doseQty.ifBlank { "1" }} ${state.doseUnit}")
            ReviewRow("Form", state.form.label)
            ReviewRow("Times", state.times.joinToString(", ") { Format.time(it) })
            ReviewRow(
                "Repeat",
                when (state.repeatMode) {
                    RepeatMode.DAILY -> "Every day"
                    RepeatMode.WEEKDAYS -> state.weekdays
                        .sortedBy { it.value }
                        .joinToString(", ") { Format.weekdayShort(it) }
                    RepeatMode.INTERVAL -> "Every ${state.intervalDays} days"
                },
            )
            ReviewRow(
                "Duration",
                when (state.durationMode) {
                    DurationMode.ONGOING -> "Ongoing"
                    DurationMode.FIXED_DAYS -> "${state.durationDays} days"
                    DurationMode.FIXED_END_DATE -> "Until ${Format.date(state.endDate)}"
                },
            )
            ReviewRow(
                "Stock",
                if (state.hasStock) {
                    "${state.stock.ifBlank { "0" }} (low at ${state.lowStockThreshold.ifBlank { "0" }})"
                } else {
                    "Not tracked"
                },
            )
            if (state.notes.isNotBlank()) ReviewRow("Notes", state.notes)
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColorPickerDialog(
    initial: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    var red by remember { mutableFloatStateOf(((initial shr 16) and 0xFF).toFloat()) }
    var green by remember { mutableFloatStateOf(((initial shr 8) and 0xFF).toFloat()) }
    var blue by remember { mutableFloatStateOf((initial and 0xFF).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom colour") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(red.roundToInt(), green.roundToInt(), blue.roundToInt())),
                )
                Spacer(Modifier.height(12.dp))
                ChannelSlider("Red", red) { red = it }
                ChannelSlider("Green", green) { green = it }
                ChannelSlider("Blue", blue) { blue = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onPick(
                        android.graphics.Color.rgb(
                            red.roundToInt(),
                            green.roundToInt(),
                            blue.roundToInt(),
                        ),
                    )
                },
            ) { Text("Use colour") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ChannelSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
            Text(value.roundToInt().toString(), style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..255f,
        )
    }
}
