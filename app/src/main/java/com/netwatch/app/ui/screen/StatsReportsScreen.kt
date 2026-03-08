package com.netwatch.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netwatch.app.R
import com.netwatch.app.core.model.WeeklyStats
import com.netwatch.app.ui.theme.NetWatchAccent
import com.netwatch.app.ui.theme.NetWatchBackground
import com.netwatch.app.ui.theme.NetWatchDanger
import com.netwatch.app.ui.theme.NetWatchPrimaryText
import com.netwatch.app.ui.theme.NetWatchSecondaryText
import com.netwatch.app.ui.theme.NetWatchSurface
import kotlin.math.max

@Composable
fun StatsReportsScreen(
    weeklyStats: WeeklyStats,
    onExportFormatted: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    exportStatus: String?,
) {
    val tabs = listOf("Daily", "Weekly", "Monthly")
    var selectedTab by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .background(NetWatchBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.netwatch_logo),
                contentDescription = "NetWatch logo",
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = "Stats & Reports",
                color = NetWatchPrimaryText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEachIndexed { index, tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (index == selectedTab) NetWatchAccent.copy(alpha = 0.22f) else NetWatchSurface,
                            shape = RoundedCornerShape(30.dp),
                        )
                        .clickable { selectedTab = index },
                ) {
                    Text(
                        text = tab,
                        color = if (index == selectedTab) NetWatchAccent else NetWatchSecondaryText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        HealthBanner(weeklyStats = weeklyStats)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Avg Download",
                value = "${"%.1f".format(weeklyStats.avgDownloadMbps)} Mbps",
                delta = if (weeklyStats.avgDownloadMbps > 40.0) "Strong" else "Low",
                deltaPositive = weeklyStats.avgDownloadMbps > 40.0,
                icon = Icons.Rounded.Download,
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Avg Upload",
                value = "${"%.1f".format(weeklyStats.avgUploadMbps)} Mbps",
                delta = if (weeklyStats.avgUploadMbps > 15.0) "Healthy" else "Bottleneck",
                deltaPositive = weeklyStats.avgUploadMbps > 15.0,
                icon = Icons.Rounded.Upload,
            )
        }

        DistributionCard(weeklyStats = weeklyStats)

        InsightCard(title = "Signal Stability", subtitle = "Switch/day ${"%.2f".format(weeklyStats.switchFrequencyPerDay)}")
        InsightCard(title = "Peak Performance", subtitle = "Latency ${"%.1f".format(weeklyStats.avgLatencyMs)} ms")
        InsightCard(title = "Data Roaming", subtitle = "Export and compare profile usage")

        Button(
            onClick = onExportFormatted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NetWatchAccent,
                contentColor = NetWatchBackground,
            ),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Text("  Export Weekly Report (Formatted)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onExportCsv,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NetWatchSurface, contentColor = NetWatchAccent),
            ) {
                Text("Export CSV")
            }
            Button(
                onClick = onExportJson,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NetWatchSurface, contentColor = NetWatchAccent),
            ) {
                Text("Export JSON")
            }
        }

        exportStatus?.let {
            Text(
                text = it,
                color = NetWatchSecondaryText,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun HealthBanner(weeklyStats: WeeklyStats) {
    val score = (
        (weeklyStats.avgDownloadMbps / 2.0) +
            (weeklyStats.avgUploadMbps / 3.0) -
            (weeklyStats.avgLatencyMs / 4.0)
        ).toInt().coerceIn(0, 100)

    Card(
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("NETWORK HEALTH", color = NetWatchSecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("$score/100", color = NetWatchPrimaryText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (score >= 70) "Stable week" else "Needs attention",
                    color = if (score >= 70) NetWatchAccent else NetWatchDanger,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(NetWatchAccent.copy(alpha = 0.16f), RoundedCornerShape(29.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Insights, contentDescription = null, tint = NetWatchAccent, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    delta: String,
    deltaPositive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NetWatchSecondaryText, modifier = Modifier.size(14.dp))
                Text("  $title", color = NetWatchSecondaryText, fontSize = 14.sp)
            }
            Text(value, color = NetWatchPrimaryText, fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
            Text(delta, color = if (deltaPositive) NetWatchAccent else NetWatchDanger, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DistributionCard(weeklyStats: WeeklyStats) {
    val totalMinutes = weeklyStats.timeOn5gMinutes + weeklyStats.timeOnLteMinutes + weeklyStats.timeOnLegacyMinutes
    val (p5g, plte, plegacy) = if (totalMinutes <= 0L) {
        Triple(1f, 1f, 1f)
    } else {
        Triple(
            max(0.15f, weeklyStats.timeOn5gMinutes / totalMinutes.toFloat()),
            max(0.15f, weeklyStats.timeOnLteMinutes / totalMinutes.toFloat()),
            max(0.15f, weeklyStats.timeOnLegacyMinutes / totalMinutes.toFloat()),
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = NetWatchSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Network Distribution", color = NetWatchPrimaryText, fontWeight = FontWeight.Bold, fontSize = 20.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(p5g)
                        .height(72.dp)
                        .background(NetWatchAccent.copy(alpha = 0.75f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text("5G", modifier = Modifier.padding(8.dp), color = NetWatchBackground, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(plte)
                        .height(72.dp)
                        .background(NetWatchAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text("LTE", modifier = Modifier.padding(8.dp), color = NetWatchPrimaryText, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(plegacy)
                        .height(72.dp)
                        .background(NetWatchAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text("3G/2G", modifier = Modifier.padding(8.dp), color = NetWatchPrimaryText, fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricText("Total 5G Time", "${weeklyStats.timeOn5gMinutes}m")
                MetricText("Switch Frequency", "${"%.2f".format(weeklyStats.switchFrequencyPerDay)} / day")
            }
        }
    }
}

@Composable
private fun MetricText(label: String, value: String) {
    Column {
        Text(label, color = NetWatchSecondaryText, fontSize = 12.sp)
        Text(value, color = NetWatchPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InsightCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = NetWatchSurface), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Speed, contentDescription = null, tint = NetWatchAccent)
            Column(modifier = Modifier.padding(horizontal = 10.dp).weight(1f)) {
                Text(title, color = NetWatchPrimaryText, fontWeight = FontWeight.Bold)
                Text(subtitle, color = NetWatchSecondaryText)
            }
            Text("›", color = NetWatchSecondaryText, fontSize = 26.sp)
        }
    }
}
