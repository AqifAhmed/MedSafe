package com.medsafe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medsafe.app.data.local.entity.ProfileEntity
import com.medsafe.app.data.model.DoseStatus
import java.util.Locale

@Composable
fun ProfileAvatar(
    profile: ProfileEntity,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(profile.color), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(profile.name),
            color = onColorFor(profile.color),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(1).uppercase(Locale.getDefault())
        else -> (words.first().take(1) + words.last().take(1)).uppercase(Locale.getDefault())
    }
}

/** Very small contrast helper for avatar text: light color -> dark text and vice versa. */
fun onColorFor(argb: Int): Color {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    return if (luminance > 0.62) Color(0xFF3B2A22) else Color(0xFFFFF8F1)
}

data class StatusVisual(
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)

@Composable
fun statusVisual(status: DoseStatus): StatusVisual {
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        DoseStatus.TAKEN -> StatusVisual(
            "Taken",
            Icons.Rounded.CheckCircle,
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
        )
        DoseStatus.MISSED -> StatusVisual(
            "Missed",
            Icons.Rounded.Cancel,
            scheme.errorContainer,
            scheme.onErrorContainer,
        )
        DoseStatus.SKIPPED -> StatusVisual(
            "Skipped",
            Icons.Rounded.RemoveCircle,
            scheme.surfaceContainerHighest,
            scheme.onSurfaceVariant,
        )
        DoseStatus.SNOOZED -> StatusVisual(
            "Snoozed",
            Icons.Rounded.Snooze,
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
        )
        DoseStatus.PENDING -> StatusVisual(
            "Due",
            Icons.Rounded.Schedule,
            scheme.secondaryContainer,
            scheme.onSecondaryContainer,
        )
    }
}

@Composable
fun DoseStatusBadge(
    status: DoseStatus,
    modifier: Modifier = Modifier,
) {
    val visual = statusVisual(status)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = visual.container,
        contentColor = visual.content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(visual.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(visual.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(18.dp)
                    .size(32.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun InfoBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.height(40.dp),
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
