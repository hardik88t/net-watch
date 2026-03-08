package com.netwatch.app.ui

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netwatch.app.data.repository.MonitoringRepository
import com.netwatch.app.data.repository.PreferencesRepository
import com.netwatch.app.export.ReportExporter
import com.netwatch.app.monitoring.NetWatchMonitorService
import com.netwatch.app.ui.navigation.NetWatchDestination
import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.MonitoringConstraints
import com.netwatch.app.core.model.SpeedTestResult
import com.netwatch.app.core.model.TimelineItem
import com.netwatch.app.core.model.WeeklyStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val monitoringRepository: MonitoringRepository,
    private val preferencesRepository: PreferencesRepository,
    private val reportExporter: ReportExporter,
) : AndroidViewModel(application) {

    val onboardingCompleted: StateFlow<Boolean> = preferencesRepository.onboardingCompleted.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    val constraints: StateFlow<MonitoringConstraints> = preferencesRepository.constraints.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MonitoringConstraints(),
    )

    val latestSnapshot: StateFlow<ConnectionSnapshot?> = monitoringRepository.observeLatestSnapshot().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val timeline: StateFlow<List<TimelineItem>> = monitoringRepository.observeTimeline(limit = 200).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val recentSpeedTests: StateFlow<List<SpeedTestResult>> = monitoringRepository.observeRecentSpeedTests().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _weeklyStats = MutableStateFlow(
        WeeklyStats(
            avgDownloadMbps = 0.0,
            avgUploadMbps = 0.0,
            avgLatencyMs = 0.0,
            timeOn5gMinutes = 0,
            timeOnLteMinutes = 0,
            timeOnLegacyMinutes = 0,
            switchFrequencyPerDay = 0.0,
        )
    )
    val weeklyStats: StateFlow<WeeklyStats> = _weeklyStats

    private val _destination = MutableStateFlow(NetWatchDestination.DASHBOARD)
    val destination: StateFlow<NetWatchDestination> = _destination

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus

    val monitoringEnabledLabel: StateFlow<String> = constraints.map { prefs ->
        if (prefs.monitoringEnabled) "Monitoring active" else "Monitoring paused"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "Monitoring paused",
    )

    init {
        refreshStatsLoop()
    }

    fun selectDestination(destination: NetWatchDestination) {
        _destination.value = destination
    }

    fun completeOnboardingAndStartMonitoring() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            preferencesRepository.setMonitoringEnabled(true)
            ContextCompat.startForegroundService(
                getApplication(),
                NetWatchMonitorService.startIntent(getApplication()),
            )
        }
    }

    fun toggleMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMonitoringEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled) {
                ContextCompat.startForegroundService(app, NetWatchMonitorService.startIntent(app))
            } else {
                app.startService(NetWatchMonitorService.stopIntent(app))
            }
        }
    }

    fun requestManualSpeedTest() {
        val app = getApplication<Application>()
        ContextCompat.startForegroundService(app, NetWatchMonitorService.manualTestIntent(app))
    }

    fun addAnnotation(eventId: Long?, note: String) {
        viewModelScope.launch {
            monitoringRepository.addAnnotation(
                eventId = eventId,
                timestampMs = System.currentTimeMillis(),
                text = note,
            )
        }
    }

    fun setDailyBudgetMb(value: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyBudgetMb(value)
        }
    }

    fun setMonthlyBudgetMb(value: Int) {
        viewModelScope.launch {
            preferencesRepository.setMonthlyBudgetMb(value)
        }
    }

    fun setMaxDuration(value: Int) {
        viewModelScope.launch {
            preferencesRepository.setMaxHeavyTestDurationSec(value)
        }
    }

    fun setCheckInterval(value: Int) {
        viewModelScope.launch {
            preferencesRepository.setLightweightCheckIntervalSec(value)
        }
    }

    fun setTriggerOnSignalDrop(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTriggerOnSignalDrop(enabled)
        }
    }

    fun setSignalDropThreshold(value: Int) {
        viewModelScope.launch {
            preferencesRepository.setSignalDropThresholdDbm(value)
        }
    }

    fun setTriggerOnTechDowngrade(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTriggerOnTechDowngrade(enabled)
        }
    }

    fun setTriggerOnDeadAir(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTriggerOnDeadAir(enabled)
        }
    }

    fun setAutoResumeOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoResumeOnBoot(enabled)
        }
    }

    fun setCompactTimelineMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCompactTimelineMode(enabled)
        }
    }

    fun setMapAutoCenter(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMapAutoCenter(enabled)
        }
    }

    fun setMapOfflineMinZoom(value: Int) {
        viewModelScope.launch {
            preferencesRepository.setMapOfflineMinZoom(value)
        }
    }

    fun setMapOfflineMaxZoom(value: Int) {
        viewModelScope.launch {
            preferencesRepository.setMapOfflineMaxZoom(value)
        }
    }

    fun exportFormattedReport() {
        viewModelScope.launch {
            runCatching { reportExporter.exportFormattedReport() }
                .onSuccess { file -> _exportStatus.value = "Formatted report: ${file.absolutePath}" }
                .onFailure { error -> _exportStatus.value = "Export failed: ${error.message}" }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            runCatching { reportExporter.exportRawCsv() }
                .onSuccess { file -> _exportStatus.value = "CSV export: ${file.absolutePath}" }
                .onFailure { error -> _exportStatus.value = "Export failed: ${error.message}" }
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            runCatching { reportExporter.exportRawJson() }
                .onSuccess { file -> _exportStatus.value = "JSON export: ${file.absolutePath}" }
                .onFailure { error -> _exportStatus.value = "Export failed: ${error.message}" }
        }
    }

    private fun refreshStatsLoop() {
        viewModelScope.launch {
            while (true) {
                _weeklyStats.value = monitoringRepository.weeklyStats(System.currentTimeMillis())
                delay(60_000)
            }
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val monitoringRepository: MonitoringRepository,
    private val preferencesRepository: PreferencesRepository,
    private val reportExporter: ReportExporter,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                application = application,
                monitoringRepository = monitoringRepository,
                preferencesRepository = preferencesRepository,
                reportExporter = reportExporter,
            ) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
