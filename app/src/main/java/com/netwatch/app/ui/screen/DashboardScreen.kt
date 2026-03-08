package com.netwatch.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netwatch.app.R
import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.SpeedTestResult
import com.netwatch.app.ui.theme.NetWatchAccent
import com.netwatch.app.ui.theme.NetWatchBackground
import com.netwatch.app.ui.theme.NetWatchPrimaryText
import com.netwatch.app.ui.theme.NetWatchSecondaryText
import com.netwatch.app.ui.theme.NetWatchSurface
import kotlin.math.abs

@Composable
fun DashboardScreen(
    snapshot: ConnectionSnapshot?,
    speedTests: List<SpeedTestResult>,
    monitoringEnabled: Boolean,
    onToggleMonitoring: (Boolean) -> Unit,
    onQuickTest: () -> Unit,
) {
    val latestSignal = snapshot?.signalDbm ?: -120

    Column(
        modifier = Modifier
            .background(NetWatchBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.netwatch_logo),
                    contentDescription = "NetWatch logo",
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    text = "NetWatch Live",
                    style = MaterialTheme.typography.titleLarge,
                    color = NetWatchPrimaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (monitoringEnabled) "ON" else "OFF",
                    color = if (monitoringEnabled) NetWatchAccent else NetWatchSecondaryText,
                    modifier = Modifier.padding(end = 4.dp),
                    fontWeight = FontWeight.Bold,
                )
                Switch(checked = monitoringEnabled, onCheckedChange = onToggleMonitoring)
            }
        }

        SignalGauge(signalDbm = latestSignal)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                MetricCard(
                    title = "Connection",
                    value = snapshot?.let { "${it.technology} - ${it.profile.displayName}" } ?: "No profile",
                    hint = if (snapshot?.hasInternet == true) "Active" else "No internet",
                    hintColor = if (snapshot?.hasInternet == true) NetWatchAccent else Color(0xFFF87171),
                    icon = Icons.Rounded.NetworkCell,
                )
            }
            item {
                val latency = speedTests.firstOrNull()?.latencyMs ?: 0.0
                MetricCard(
                    title = "Latency",
                    value = "${"%.1f".format(latency)} ms",
                    hint = if (speedTests.firstOrNull()?.estimated == true) "Estimated" else "Measured",
                    hintColor = if (speedTests.firstOrNull()?.estimated == true) NetWatchSecondaryText else NetWatchAccent,
                    icon = Icons.Rounded.NetworkCheck,
                )
            }
            items(speedTests.take(2)) { result ->
                MetricCard(
                    title = "${result.triggerReason}",
                    value = "${"%.1f".format(result.downloadMbps)}↓ ${"%.1f".format(result.uploadMbps)}↑",
                    hint = if (result.estimated) "Estimated" else "Sample",
                    hintColor = if (result.estimated) NetWatchSecondaryText else NetWatchAccent,
                    icon = Icons.Rounded.Bolt,
                )
            }
        }

        SignalStabilityCard(speedTests = speedTests)

        Button(
            onClick = onQuickTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NetWatchAccent,
                contentColor = NetWatchBackground,
            ),
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null)
            Text(
                text = "  Quick Test Now",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
private fun SignalGauge(signalDbm: Int) {
    val normalized = ((signalDbm + 120) / 70f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(NetWatchSurface, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(230.dp)) {
            drawCircle(
                color = NetWatchAccent.copy(alpha = 0.1f),
                radius = size.minDimension / 2,
                style = Stroke(width = 20f),
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(NetWatchAccent.copy(alpha = 0.35f), NetWatchAccent, NetWatchAccent)
                ),
                startAngle = -90f,
                sweepAngle = 360f * normalized,
                useCenter = false,
                size = Size(size.width, size.height),
                style = Stroke(width = 18f, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SIGNAL", color = NetWatchAccent, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("$signalDbm", color = NetWatchPrimaryText, fontSize = 58.sp, fontWeight = FontWeight.Bold)
            Text("dBm", color = NetWatchSecondaryText, fontSize = 28.sp)
            Text(
                text = when {
                    signalDbm >= -80 -> "Excellent"
                    signalDbm >= -95 -> "Stable"
                    signalDbm >= -105 -> "Weak"
                    else -> "Critical"
                },
                color = NetWatchAccent,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(NetWatchAccent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    hint: String,
    hintColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        modifier = Modifier
            .height(118.dp)
            .fillMaxWidth(0.48f),
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NetWatchSecondaryText, modifier = Modifier.size(14.dp))
                Text(
                    text = "  ${title.uppercase()}",
                    color = NetWatchSecondaryText,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(text = value, color = NetWatchPrimaryText, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 28.sp)
            Text(text = hint, color = hintColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SignalStabilityCard(speedTests: List<SpeedTestResult>) {
    val points = if (speedTests.size >= 6) {
        speedTests.take(6).map { abs(it.latencyMs - 90.0).toFloat() }
    } else {
        listOf(35f, 52f, 48f, 60f, 44f, 67f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SIGNAL STABILITY", color = NetWatchSecondaryText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    text = "+${"%.1f".format((100 - points.average()).coerceAtLeast(0.0))}%",
                    color = NetWatchAccent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "${"%.1f".format(points.average() * -1)} dBm avg",
                color = NetWatchPrimaryText,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )

            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)) {
                val step = size.width / (points.size - 1)
                for (index in 0 until points.lastIndex) {
                    val start = Offset(x = index * step, y = size.height - points[index])
                    val end = Offset(x = (index + 1) * step, y = size.height - points[index + 1])
                    drawLine(
                        color = NetWatchAccent,
                        start = start,
                        end = end,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("10M AGO", color = NetWatchSecondaryText, fontSize = 11.sp)
                Text("5M AGO", color = NetWatchSecondaryText, fontSize = 11.sp)
                Text("NOW", color = NetWatchAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
