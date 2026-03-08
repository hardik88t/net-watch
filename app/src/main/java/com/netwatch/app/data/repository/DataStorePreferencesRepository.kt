package com.netwatch.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.netwatch.app.core.model.MonitoringConstraints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "netwatch_prefs")

class DataStorePreferencesRepository(
    context: Context,
) : PreferencesRepository {

    private val store = context.dataStore

    override val constraints: Flow<MonitoringConstraints> = store.data.map { prefs ->
        MonitoringConstraints(
            monitoringEnabled = prefs[Keys.monitoringEnabled] ?: true,
            autoResumeOnBoot = prefs[Keys.autoResumeOnBoot] ?: true,
            dailyBudgetMb = prefs[Keys.dailyBudgetMb] ?: 512,
            monthlyBudgetMb = prefs[Keys.monthlyBudgetMb] ?: 5_120,
            maxHeavyTestDurationSec = prefs[Keys.maxHeavyTestDurationSec] ?: 20,
            lightweightCheckIntervalSec = prefs[Keys.lightweightCheckIntervalSec] ?: 60,
            triggerOnSignalDrop = prefs[Keys.triggerOnSignalDrop] ?: true,
            signalDropThresholdDbm = prefs[Keys.signalDropThresholdDbm] ?: 20,
            triggerOnTechDowngrade = prefs[Keys.triggerOnTechDowngrade] ?: true,
            triggerOnDeadAir = prefs[Keys.triggerOnDeadAir] ?: true,
        )
    }

    override val onboardingCompleted: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.onboardingCompleted] ?: false
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) = store.write(Keys.monitoringEnabled, enabled)
    override suspend fun setAutoResumeOnBoot(enabled: Boolean) = store.write(Keys.autoResumeOnBoot, enabled)
    override suspend fun setDailyBudgetMb(value: Int) = store.write(Keys.dailyBudgetMb, value)
    override suspend fun setMonthlyBudgetMb(value: Int) = store.write(Keys.monthlyBudgetMb, value)
    override suspend fun setMaxHeavyTestDurationSec(value: Int) = store.write(Keys.maxHeavyTestDurationSec, value)
    override suspend fun setLightweightCheckIntervalSec(value: Int) = store.write(Keys.lightweightCheckIntervalSec, value)
    override suspend fun setTriggerOnSignalDrop(enabled: Boolean) = store.write(Keys.triggerOnSignalDrop, enabled)
    override suspend fun setSignalDropThresholdDbm(value: Int) = store.write(Keys.signalDropThresholdDbm, value)
    override suspend fun setTriggerOnTechDowngrade(enabled: Boolean) = store.write(Keys.triggerOnTechDowngrade, enabled)
    override suspend fun setTriggerOnDeadAir(enabled: Boolean) = store.write(Keys.triggerOnDeadAir, enabled)
    override suspend fun setOnboardingCompleted(completed: Boolean) = store.write(Keys.onboardingCompleted, completed)

    private suspend fun <T> DataStore<Preferences>.write(key: Preferences.Key<T>, value: T) {
        edit { prefs ->
            prefs[key] = value
        }
    }

    private object Keys {
        val monitoringEnabled = booleanPreferencesKey("monitoring_enabled")
        val autoResumeOnBoot = booleanPreferencesKey("auto_resume_on_boot")
        val dailyBudgetMb = intPreferencesKey("daily_budget_mb")
        val monthlyBudgetMb = intPreferencesKey("monthly_budget_mb")
        val maxHeavyTestDurationSec = intPreferencesKey("max_heavy_test_duration_sec")
        val lightweightCheckIntervalSec = intPreferencesKey("lightweight_check_interval_sec")
        val triggerOnSignalDrop = booleanPreferencesKey("trigger_on_signal_drop")
        val signalDropThresholdDbm = intPreferencesKey("signal_drop_threshold_dbm")
        val triggerOnTechDowngrade = booleanPreferencesKey("trigger_on_tech_downgrade")
        val triggerOnDeadAir = booleanPreferencesKey("trigger_on_dead_air")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
    }
}
