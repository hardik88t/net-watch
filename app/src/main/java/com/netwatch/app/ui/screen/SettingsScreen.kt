package com.netwatch.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netwatch.app.core.model.MonitoringConstraints
import com.netwatch.app.ui.theme.NetWatchBackground
import com.netwatch.app.ui.theme.NetWatchPrimaryText
import com.netwatch.app.ui.theme.NetWatchSecondaryText
import com.netwatch.app.ui.theme.NetWatchSurface

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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetWatchBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Monitoring Preferences",
            color = NetWatchPrimaryText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Define strict budgets, trigger rules, and automation behavior.",
            color = NetWatchSecondaryText,
            fontSize = 14.sp,
        )

        ToggleCard(
            title = "Monitoring Enabled",
            description = "Pause or resume passive tracking and anomaly checks",
            checked = constraints.monitoringEnabled,
            onCheckedChange = onMonitoringToggle,
        )
        ToggleCard(
            title = "Auto Resume On Boot",
            description = "Restore configured monitoring state after reboot",
            checked = constraints.autoResumeOnBoot,
            onCheckedChange = onAutoResumeToggle,
        )

        SliderCard(
            label = "Daily heavy-test budget",
            value = constraints.dailyBudgetMb.toFloat(),
            valueRange = 64f..4096f,
            steps = 14,
            valueSuffix = "MB",
            onValueChange = { onDailyBudgetChange(it.toInt()) },
        )

        SliderCard(
            label = "Monthly heavy-test budget",
            value = constraints.monthlyBudgetMb.toFloat(),
            valueRange = 512f..20_480f,
            steps = 16,
            valueSuffix = "MB",
            onValueChange = { onMonthlyBudgetChange(it.toInt()) },
        )

        SliderCard(
            label = "Max heavy-test duration",
            value = constraints.maxHeavyTestDurationSec.toFloat(),
            valueRange = 5f..120f,
            steps = 22,
            valueSuffix = "sec",
            onValueChange = { onMaxDurationChange(it.toInt()) },
        )

        SliderCard(
            label = "Lightweight check interval",
            value = constraints.lightweightCheckIntervalSec.toFloat(),
            valueRange = 15f..300f,
            steps = 18,
            valueSuffix = "sec",
            onValueChange = { onCheckIntervalChange(it.toInt()) },
        )

        ToggleCard(
            title = "Trigger on signal drop",
            description = "Start heavy test when dBm suddenly degrades",
            checked = constraints.triggerOnSignalDrop,
            onCheckedChange = onSignalDropToggle,
        )

        SliderCard(
            label = "Signal drop threshold",
            value = constraints.signalDropThresholdDbm.toFloat(),
            valueRange = 5f..40f,
            steps = 6,
            valueSuffix = "dBm",
            onValueChange = { onSignalDropThresholdChange(it.toInt()) },
        )

        ToggleCard(
            title = "Trigger on LTE/5G downgrade",
            description = "Run heavy diagnostics when network technology rank drops",
            checked = constraints.triggerOnTechDowngrade,
            onCheckedChange = onTechDowngradeToggle,
        )

        ToggleCard(
            title = "Trigger on dead-air anomalies",
            description = "Run heavy diagnostics when probe fails despite strong signal",
            checked = constraints.triggerOnDeadAir,
            onCheckedChange = onDeadAirToggle,
        )
    }
}

@Composable
private fun ToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = NetWatchSurface), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
) {
    Card(colors = CardDefaults.cardColors(containerColor = NetWatchSurface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = NetWatchPrimaryText, fontWeight = FontWeight.SemiBold)
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
