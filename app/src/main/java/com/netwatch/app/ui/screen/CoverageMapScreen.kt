package com.netwatch.app.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.max

@Composable
fun CoverageMapScreen(
    timeline: List<TimelineItem>,
) {
    val points = timeline
        .asSequence()
        .mapNotNull { item ->
            val lat = item.event.latitude ?: return@mapNotNull null
            val lon = item.event.longitude ?: return@mapNotNull null
            CoveragePoint(
                latitude = lat,
                longitude = lon,
                signalDbm = item.event.signalDbm,
                type = item.event.type,
                message = item.event.message,
            )
        }
        .take(80)
        .toList()

    val averageSignal = points.mapNotNull { it.signalDbm }.average().takeIf { !it.isNaN() } ?: -110.0
    val coverageScore = ((averageSignal + 120.0) / 0.6).toInt().coerceIn(0, 100)
    val outageCount = points.count { it.type == NetworkEventType.OUTAGE_START }

    LazyColumn(
        modifier = Modifier
            .background(NetWatchBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Coverage Map",
                color = NetWatchPrimaryText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            Text(
                text = "Geotagged transitions, outages, and anomalies rendered from local timeline logs.",
                color = NetWatchSecondaryText,
                fontSize = 14.sp,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = "Coverage Score",
                    value = "$coverageScore/100",
                    icon = Icons.Rounded.MyLocation,
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = "Outage Pins",
                    value = outageCount.toString(),
                    icon = Icons.Rounded.NetworkCell,
                )
            }
        }

        item {
            CoverageMapCard(points = points)
        }

        item {
            Text(
                text = "Recent mapped events",
                color = NetWatchPrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }

        if (points.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "No geotagged points yet",
                            color = NetWatchPrimaryText,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Grant location access and let monitoring run to build the coverage map.",
                            color = NetWatchSecondaryText,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        } else {
            items(points.take(12)) { point ->
                LocationEventRow(point = point)
            }
        }
    }
}

@Composable
private fun CoverageMapCard(points: List<CoveragePoint>) {
    val infiniteTransition = rememberInfiniteTransition(label = "coverage-map-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coverage-pulse-scale",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NetWatchAccent.copy(alpha = 0.1f), NetWatchSurface),
                    )
                ),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val minLat = points.minOfOrNull { it.latitude } ?: 0.0
                val maxLat = points.maxOfOrNull { it.latitude } ?: 1.0
                val minLon = points.minOfOrNull { it.longitude } ?: 0.0
                val maxLon = points.maxOfOrNull { it.longitude } ?: 1.0

                val latSpan = max(0.00001, maxLat - minLat)
                val lonSpan = max(0.00001, maxLon - minLon)

                repeat(5) { index ->
                    val y = size.height * (index / 4f)
                    drawLine(
                        color = NetWatchAccent.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round,
                    )
                }

                points.forEach { point ->
                    val x = ((point.longitude - minLon) / lonSpan).toFloat() * size.width
                    val y = size.height - (((point.latitude - minLat) / latSpan).toFloat() * size.height)
                    val baseRadius = if (point.type == NetworkEventType.OUTAGE_START) 11f else 8f
                    val pointColor = if (point.type == NetworkEventType.OUTAGE_START) {
                        NetWatchDanger
                    } else {
                        NetWatchAccent
                    }

                    drawCircle(
                        color = pointColor.copy(alpha = 0.20f * pulseScale),
                        radius = baseRadius * 2.3f * pulseScale,
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = pointColor,
                        radius = baseRadius,
                        center = Offset(x, y),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = NetWatchAccent, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(text = title, color = NetWatchSecondaryText, fontSize = 12.sp)
                Text(text = value, color = NetWatchPrimaryText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationEventRow(point: CoveragePoint) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = if (point.type == NetworkEventType.OUTAGE_START) NetWatchDanger else NetWatchAccent,
            )
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(
                    text = point.message,
                    color = NetWatchPrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)}",
                    color = NetWatchSecondaryText,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = point.signalDbm?.let { "$it dBm" } ?: "-- dBm",
                color = NetWatchAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private data class CoveragePoint(
    val latitude: Double,
    val longitude: Double,
    val signalDbm: Int?,
    val type: NetworkEventType,
    val message: String,
)
