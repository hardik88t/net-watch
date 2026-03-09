package com.netwatch.app.core.model

data class MonitoringConstraints(
    val monitoringEnabled: Boolean = true,
    val autoResumeOnBoot: Boolean = true,
    val dailyBudgetMb: Int = 512,
    val monthlyBudgetMb: Int = 5_120,
    val maxHeavyTestDurationSec: Int = 20,
    val lightweightCheckIntervalSec: Int = 60,
    val triggerOnSignalDrop: Boolean = true,
    val signalDropThresholdDbm: Int = 20,
    val triggerOnTechDowngrade: Boolean = true,
    val triggerOnDeadAir: Boolean = true,
    val compactTimelineMode: Boolean = false,
    val mapAutoCenter: Boolean = true,
    val globalFontSize: String = "Base",
)
