@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.medsafe.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsafe.app.data.model.DoseStatus
import com.medsafe.app.ui.components.ClayCard
import com.medsafe.app.ui.components.DoseStatusBadge
import com.medsafe.app.ui.components.PillIcon
import com.medsafe.app.util.Format
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("History", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = state.profile?.let { "${it.name}'s adherence" } ?: "Adherence",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthSummaryCard(
                    monthTaken = state.monthTaken,
                    monthMissed = state.monthMissed,
                    monthSkipped = state.monthSkipped,
                )
            }
            item {
                CalendarCard(
                    state = state,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    onToday = viewModel::goToCurrentMonth,
                    onSelectDate = viewModel::selectDate,
                )
            }
            item {
                Text(
                    text = state.selectedDate.format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (state.selectedLogs.isEmpty()) {
                item {
                    Text(
                        text = "No doses recorded for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.selectedLogs, key = { it.log.id }) { entry ->
                    ClayCard(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PillIcon(
                                shape = entry.medication.pillShape,
                                color1 = Color(entry.medication.pillColor1),
                                color2 = entry.medication.pillColor2?.let { Color(it) },
                                size = 38.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.medication.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "${Format.time(java.time.Instant.ofEpochMilli(entry.log.scheduledAt).atZone(java.time.ZoneId.systemDefault()).toLocalTime())} · ${Format.doseAmount(entry.medication)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DoseStatusBadge(entry.log.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(
    monthTaken: Int,
    monthMissed: Int,
    monthSkipped: Int,
) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("This month", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryStat(
                    icon = Icons.Rounded.CheckCircle,
                    value = monthTaken,
                    label = "Taken",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                SummaryStat(
                    icon = Icons.Rounded.Cancel,
                    value = monthMissed,
                    label = "Missed",
                    tint = MaterialTheme.colorScheme.error,
                )
                SummaryStat(
                    icon = Icons.Rounded.RemoveCircle,
                    value = monthSkipped,
                    label = "Skipped",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    label: String,
    tint: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarCard(
    state: HistoryUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    ClayCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    text = state.month.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
                }
            }
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.values().forEach { day ->
                    Text(
                        text = Format.weekdayShort(day).take(1),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            CalendarGrid(state = state, onSelectDate = onSelectDate)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(color = MaterialTheme.colorScheme.tertiary, label = "Taken")
                LegendDot(color = MaterialTheme.colorScheme.error, label = "Missed")
                LegendDot(color = MaterialTheme.colorScheme.outline, label = "Skipped")
            }
            TextButton(onClick = onToday, modifier = Modifier.height(40.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Jump to today")
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    state: HistoryUiState,
    onSelectDate: (LocalDate) -> Unit,
) {
    val firstDay = state.month.atDay(1)
    val leading = firstDay.dayOfWeek.value - 1
    val daysInMonth = state.month.lengthOfMonth()
    val rows = ceil((leading + daysInMonth) / 7.0).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - leading + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                    ) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = state.month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                day = state.days[date],
                                isSelected = date == state.selectedDate,
                                onClick = { onSelectDate(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    day: HistoryDay?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isToday = date == LocalDate.now()
    val container = when {
        day == null -> Color.Transparent
        day.missed > 0 -> MaterialTheme.colorScheme.errorContainer
        day.taken > 0 && day.taken == day.total -> MaterialTheme.colorScheme.tertiaryContainer
        day.taken > 0 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val border = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.outline
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(
                width = if (isSelected || isToday) 2.dp else 1.dp,
                color = border,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (day != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (day.taken > 0) Dot(MaterialTheme.colorScheme.tertiary)
                    if (day.missed > 0) Dot(MaterialTheme.colorScheme.error)
                    if (day.skipped > 0) Dot(MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
