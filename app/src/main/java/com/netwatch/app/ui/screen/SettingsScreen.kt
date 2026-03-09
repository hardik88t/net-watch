package com.netwatch.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.netwatch.app.core.model.MonitoringConstraints
import com.netwatch.app.ui.theme.NetWatchAccent
import com.netwatch.app.ui.theme.NetWatchBackground
import com.netwatch.app.ui.theme.NetWatchPrimaryText
import com.netwatch.app.ui.theme.NetWatchSecondaryText
import com.netwatch.app.ui.theme.NetWatchSurface

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SettingsScreen(
    constraints: MonitoringConstraints,
    onMonitoringToggle: (Boolean) -> Unit,
    onAutoResumeToggle: (Boolean) -> Unit,
    onDailyBudgetChange: (Int) -> Unit,
    onMonthlyBudgetChange: (Int) -> Unit,
    onMaxDurationChange: (Int) -> Unit,
    onCheckIntervalChange: (Int) -> Unit,
    onSignalDropToggle: (Boolean) -> Unit,
    onSignalDropThresholdChange: (Int) -> Unit,
    onTechDowngradeToggle: (Boolean) -> Unit,
    onDeadAirToggle: (Boolean) -> Unit,
    onCompactTimelineModeToggle: (Boolean) -> Unit,
    onMapAutoCenterToggle: (Boolean) -> Unit,
    onGlobalFontSizeChange: (String) -> Unit,
    onStartHeavyTest: () -> Unit,
) {
    var showHeavyTestWarning by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetWatchBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.netwatch_logo),
                contentDescription = "NetWatch logo",
                modifier = Modifier
                    .size(30.dp)
                    .padding(end = 8.dp),
            )
            Text(
                text = "Monitoring Preferences",
                color = NetWatchPrimaryText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "Define strict budgets, trigger rules, and automation behavior.",
            color = NetWatchSecondaryText,
            fontSize = 14.sp,
        )

        SectionTitle("Runtime Controls")

        ToggleCard(
            title = "Monitoring Enabled",
            description = "Pause or resume passive tracking and anomaly checks",
            checked = constraints.monitoringEnabled,
            onCheckedChange = onMonitoringToggle,
            icon = Icons.Rounded.SettingsSuggest,
        )
        ToggleCard(
            title = "Auto Resume On Boot",
            description = "Restore configured monitoring state after reboot",
            checked = constraints.autoResumeOnBoot,
            onCheckedChange = onAutoResumeToggle,
            icon = Icons.Rounded.Schedule,
        )

        SectionTitle("Data Budgets")

        SliderCard(
            label = "Daily heavy-test budget",
            value = constraints.dailyBudgetMb.toFloat(),
            valueRange = 64f..4096f,
            steps = 14,
            valueSuffix = "MB",
            onValueChange = { onDailyBudgetChange(it.toInt()) },
            icon = Icons.Rounded.DataUsage,
        )

        SliderCard(
            label = "Monthly heavy-test budget",
            value = constraints.monthlyBudgetMb.toFloat(),
            valueRange = 512f..20_480f,
            steps = 16,
            valueSuffix = "MB",
            onValueChange = { onMonthlyBudgetChange(it.toInt()) },
            icon = Icons.Rounded.DataUsage,
        )

        SectionTitle("Test Scheduling")

        SliderCard(
            label = "Max heavy-test duration",
            value = constraints.maxHeavyTestDurationSec.toFloat(),
            valueRange = 5f..120f,
            steps = 22,
            valueSuffix = "sec",
            onValueChange = { onMaxDurationChange(it.toInt()) },
            icon = Icons.Rounded.Schedule,
        )

        SliderCard(
            label = "Lightweight check interval",
            value = constraints.lightweightCheckIntervalSec.toFloat(),
            valueRange = 15f..300f,
            steps = 18,
            valueSuffix = "sec",
            onValueChange = { onCheckIntervalChange(it.toInt()) },
            icon = Icons.Rounded.Schedule,
        )

        SectionTitle("Trigger Rules")

        ToggleCard(
            title = "Trigger on signal drop",
            description = "Start heavy test when dBm suddenly degrades",
            checked = constraints.triggerOnSignalDrop,
            onCheckedChange = onSignalDropToggle,
            icon = Icons.Rounded.Bolt,
        )

        SliderCard(
            label = "Signal drop threshold",
            value = constraints.signalDropThresholdDbm.toFloat(),
            valueRange = 5f..40f,
            steps = 6,
            valueSuffix = "dBm",
            onValueChange = { onSignalDropThresholdChange(it.toInt()) },
            icon = Icons.Rounded.Bolt,
        )

        ToggleCard(
            title = "Trigger on LTE/5G downgrade",
            description = "Run heavy diagnostics when network technology rank drops",
            checked = constraints.triggerOnTechDowngrade,
            onCheckedChange = onTechDowngradeToggle,
            icon = Icons.Rounded.Bolt,
        )

        ToggleCard(
            title = "Trigger on dead-air anomalies",
            description = "Run heavy diagnostics when probe fails despite strong signal",
            checked = constraints.triggerOnDeadAir,
            onCheckedChange = onDeadAirToggle,
            icon = Icons.Rounded.Bolt,
        )

        SectionTitle("Personalization")

        val fontOptions = listOf("Small", "Base", "Big")
        val fontSelectedIndex = fontOptions.indexOf(constraints.globalFontSize).takeIf { it >= 0 } ?: 1

        Card(colors = CardDefaults.cardColors(containerColor = NetWatchSurface), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Global Font Size", color = NetWatchPrimaryText, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    fontOptions.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = fontOptions.size),
                            onClick = { onGlobalFontSizeChange(label) },
                            selected = index == fontSelectedIndex
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        ToggleCard(
            title = "Compact timeline cards",
            description = "Use denser timeline cards for faster scanning",
            checked = constraints.compactTimelineMode,
            onCheckedChange = onCompactTimelineModeToggle,
            icon = Icons.Rounded.SettingsSuggest,
        )

        ToggleCard(
            title = "Map auto-center",
            description = "Auto-center map on latest geotagged point",
            checked = constraints.mapAutoCenter,
            onCheckedChange = onMapAutoCenterToggle,
            icon = Icons.Rounded.Map,
        )

        SectionTitle("Diagnostics")

        Button(
            onClick = {
                if (!constraints.monitoringEnabled) {
                    showHeavyTestWarning = true
                } else {
                    onStartHeavyTest()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NetWatchAccent, contentColor = NetWatchBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Start Heavy Test Now", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        }

    }

    if (showHeavyTestWarning) {
        AlertDialog(
            onDismissRequest = { showHeavyTestWarning = false },
            title = { Text("Monitoring Paused") },
            text = { Text("Passive monitoring is currently paused. Are you sure you still want to run a heavy performance test?") },
            confirmButton = {
                Button(onClick = {
                    showHeavyTestWarning = false
                    onStartHeavyTest()
                }) {
                    Text("Run Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHeavyTestWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = NetWatchAccent,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun ToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(colors = CardDefaults.cardColors(containerColor = NetWatchSurface), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NetWatchAccent,
                modifier = Modifier.padding(end = 10.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = NetWatchPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = NetWatchSecondaryText, fontSize = 13.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SliderCard(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueSuffix: String,
    onValueChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(colors = CardDefaults.cardColors(containerColor = NetWatchSurface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = NetWatchAccent, modifier = Modifier.padding(end = 8.dp))
                Text(label, color = NetWatchPrimaryText, fontWeight = FontWeight.SemiBold)
            }
            Text("${value.toInt()} $valueSuffix", color = NetWatchSecondaryText)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
            )
        }
    }
}
