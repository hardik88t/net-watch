package com.netwatch.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.core.model.TimelineItem
import com.netwatch.app.ui.theme.NetWatchAccent
import com.netwatch.app.ui.theme.NetWatchBackground
import com.netwatch.app.ui.theme.NetWatchDanger
import com.netwatch.app.ui.theme.NetWatchPrimaryText
import com.netwatch.app.ui.theme.NetWatchSecondaryText
import com.netwatch.app.ui.theme.NetWatchSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    items: List<TimelineItem>,
    onAddNote: (Long?, String) -> Unit,
    compactMode: Boolean,
) {
    var noteDialogItem by remember { mutableStateOf<TimelineItem?>(null) }
    var selectedFilter by remember { mutableStateOf(TimelineFilter.ALL) }

    val filteredItems = remember(items, selectedFilter) {
        items.filter { timelineItem ->
            when (selectedFilter) {
                TimelineFilter.ALL -> true
                TimelineFilter.OUTAGES -> timelineItem.event.type == NetworkEventType.OUTAGE_START || timelineItem.event.type == NetworkEventType.OUTAGE_END
                TimelineFilter.SPEED_TESTS -> timelineItem.event.type == NetworkEventType.SPEED_TEST
                TimelineFilter.ANOMALIES -> timelineItem.event.type == NetworkEventType.ANOMALY
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetWatchBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Network Event Timeline",
            color = NetWatchPrimaryText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Chronological transitions, outages, anomalies, and tests with geotag context.",
            color = NetWatchSecondaryText,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${filteredItems.size} events",
                color = NetWatchAccent,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = NetWatchSecondaryText)
                Text(
                    text = "Filters",
                    color = NetWatchSecondaryText,
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 12.sp,
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TimelineFilter.entries) { filter ->
                val selected = filter == selectedFilter
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) NetWatchAccent.copy(alpha = 0.2f) else NetWatchSurface
                    ),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.clickable { selectedFilter = filter },
                ) {
                    Text(
                        text = filter.label,
                        color = if (selected) NetWatchAccent else NetWatchSecondaryText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "No logs yet. Enable monitoring or run a manual test.",
                    modifier = Modifier.padding(16.dp),
                    color = NetWatchSecondaryText,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compactMode) 8.dp else 10.dp),
            ) {
                items(filteredItems, key = { it.event.id }) { item ->
                    TimelineItemCard(
                        item = item,
                        compactMode = compactMode,
                        onAddNote = { noteDialogItem = item },
                    )
                }
            }
        }
    }

    NoteDialog(
        target = noteDialogItem,
        onDismiss = { noteDialogItem = null },
        onSave = { target, note ->
            onAddNote(target.event.id, note)
            noteDialogItem = null
        },
    )
}

@Composable
private fun TimelineItemCard(
    item: TimelineItem,
    compactMode: Boolean,
    onAddNote: () -> Unit,
) {
    val event = item.event
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    val statusColor = when (event.type) {
        NetworkEventType.OUTAGE_START -> NetWatchDanger
        NetworkEventType.OUTAGE_END -> NetWatchAccent
        NetworkEventType.ANOMALY -> Color(0xFFFBBF24)
        NetworkEventType.SPEED_TEST -> Color(0xFF22D3EE)
        else -> NetWatchSecondaryText
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(if (compactMode) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = event.type.name.replace('_', ' '),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatter.format(Date(event.timestampMs)),
                    color = NetWatchSecondaryText,
                    fontSize = 12.sp,
                )
            }

            Text(
                text = event.message,
                color = NetWatchPrimaryText,
                fontSize = if (compactMode) 15.sp else 17.sp,
                lineHeight = if (compactMode) 20.sp else 24.sp,
            )
            Text(
                text = buildString {
                    append(event.profileKey ?: "unknown profile")
                    event.signalDbm?.let { append("  |  $it dBm") }
                    if (event.latitude != null && event.longitude != null) {
                        append("  |  ${"%.5f".format(event.latitude)}, ${"%.5f".format(event.longitude)}")
                    }
                    event.durationMs?.let { append("  |  ${it / 1000}s") }
                },
                color = NetWatchSecondaryText,
                fontSize = 12.sp,
            )

            item.note?.let { note ->
                Text(
                    text = "Note: $note",
                    color = NetWatchAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NetWatchAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(if (compactMode) 6.dp else 8.dp),
                )
            }

            TextButton(onClick = onAddNote) {
                Text("Add Manual Annotation")
            }
        }
    }
}

@Composable
private fun NoteDialog(
    target: TimelineItem?,
    onDismiss: () -> Unit,
    onSave: (TimelineItem, String) -> Unit,
) {
    if (target == null) {
        return
    }

    var text by remember(target.event.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add context note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Attach user context to this event. Example: entered basement, train tunnel, office elevator.",
                    color = NetWatchSecondaryText,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(target, text.trim())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private enum class TimelineFilter(
    val label: String,
) {
    ALL("All"),
    OUTAGES("Outages"),
    SPEED_TESTS("Tests"),
    ANOMALIES("Anomalies"),
}
