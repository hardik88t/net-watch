package com.netwatch.app.data.repository

import com.netwatch.app.core.model.MonitoringConstraints
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val constraints: Flow<MonitoringConstraints>

    suspend fun setMonitoringEnabled(enabled: Boolean)
    suspend fun setAutoResumeOnBoot(enabled: Boolean)
    suspend fun setDailyBudgetMb(value: Int)
    suspend fun setMonthlyBudgetMb(value: Int)
    suspend fun setMaxHeavyTestDurationSec(value: Int)
    suspend fun setLightweightCheckIntervalSec(value: Int)
    suspend fun setTriggerOnSignalDrop(enabled: Boolean)
    suspend fun setSignalDropThresholdDbm(value: Int)
    suspend fun setTriggerOnTechDowngrade(enabled: Boolean)
    suspend fun setTriggerOnDeadAir(enabled: Boolean)
    suspend fun setCompactTimelineMode(enabled: Boolean)
    suspend fun setMapAutoCenter(enabled: Boolean)
    suspend fun setMapOfflineMinZoom(value: Int)
    suspend fun setMapOfflineMaxZoom(value: Int)
    suspend fun setOnboardingCompleted(completed: Boolean)
    val onboardingCompleted: Flow<Boolean>
}
