package com.netwatch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier // <-- Added for compose imports
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netwatch.app.ui.navigation.NetWatchDestination
import com.netwatch.app.ui.screen.CoverageMapScreen
import com.netwatch.app.ui.screen.DashboardScreen
import com.netwatch.app.ui.screen.OnboardingScreen
import com.netwatch.app.ui.screen.SettingsScreen
import com.netwatch.app.ui.screen.StatsReportsScreen
import com.netwatch.app.ui.screen.TimelineScreen
import com.netwatch.app.ui.theme.NetWatchBackground

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun NetWatchApp(
    viewModel: MainViewModel,
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    if (!onboardingCompleted) {
        OnboardingScreen(
            onGrantAccess = viewModel::completeOnboardingAndStartMonitoring,
            onSkip = viewModel::completeOnboardingAndStartMonitoring,
        )
        return
    }

    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val latestSnapshot by viewModel.latestSnapshot.collectAsStateWithLifecycle()
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val constraints by viewModel.constraints.collectAsStateWithLifecycle()
    val recentSpeedTests by viewModel.recentSpeedTests.collectAsStateWithLifecycle()
    val timeScopedStats by viewModel.timeScopedStats.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.exportFileEvent.collect { file ->
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = when (file.extension) {
                        "pdf" -> "application/pdf"
                        "csv" -> "text/csv"
                        "json" -> "application/json"
                        else -> "text/plain"
                    }
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export Report"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NetWatchBackground,
        bottomBar = {
            NavigationBar(
                containerColor = NetWatchBackground.copy(alpha = 0.95f),
            ) {
                NetWatchDestination.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == destination,
                        onClick = { viewModel.selectDestination(tab) },
                        icon = {
                            when (tab) {
                                NetWatchDestination.DASHBOARD -> Icon(Icons.Rounded.Dashboard, contentDescription = tab.label)
                                NetWatchDestination.MAP -> Icon(Icons.Rounded.Map, contentDescription = tab.label)
                                NetWatchDestination.TIMELINE -> Icon(Icons.Rounded.Timeline, contentDescription = tab.label)
                                NetWatchDestination.STATS -> Icon(Icons.Rounded.BarChart, contentDescription = tab.label)
                                NetWatchDestination.SETTINGS -> Icon(Icons.Rounded.Settings, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(NetWatchBackground, Color(0xFF0C1C19), NetWatchBackground),
                    )
                )
                .padding(innerPadding)
        ) {
            when (destination) {
                NetWatchDestination.DASHBOARD -> {
                    val isTestRunning by viewModel.isTestRunning.collectAsStateWithLifecycle()
                    DashboardScreen(
                        snapshot = latestSnapshot,
                        speedTests = recentSpeedTests,
                        monitoringEnabled = constraints.monitoringEnabled,
                        isTestRunning = isTestRunning,
                        onToggleMonitoring = viewModel::toggleMonitoring,
                        onQuickTest = viewModel::requestManualSpeedTest,
                    )
                }

                NetWatchDestination.MAP -> CoverageMapScreen(
                    timeline = timeline,
                    autoCenter = constraints.mapAutoCenter,
                )

                NetWatchDestination.TIMELINE -> TimelineScreen(
                    items = timeline,
                    onAddNote = viewModel::addAnnotation,
                    onDeleteNote = viewModel::deleteAnnotation,
                    onDeleteEvent = viewModel::deleteEvent,
                    onMarkException = viewModel::markEventAsException,
                    compactMode = constraints.compactTimelineMode,
                )

                NetWatchDestination.STATS -> {
                    val stats by viewModel.timeScopedStats.collectAsStateWithLifecycle()
                    val statsRange by viewModel.statsTimeRangeDays.collectAsStateWithLifecycle()
                    StatsReportsScreen(
                        timeScopedStats = stats,
                        statsTimeRangeDays = statsRange,
                        onExportFormatted = viewModel::exportFormattedReport,
                        onExportCsv = viewModel::exportCsv,
                        onExportJson = viewModel::exportJson,
                        onExportPdf = viewModel::exportPdf,
                        onTabSelected = viewModel::setStatsTimeRangeDays,
                    )
                }
                NetWatchDestination.SETTINGS -> SettingsScreen(
                    constraints = constraints,
                    onMonitoringToggle = viewModel::toggleMonitoring,
                    onAutoResumeToggle = viewModel::setAutoResumeOnBoot,
                    onDailyBudgetChange = viewModel::setDailyBudgetMb,
                    onMonthlyBudgetChange = viewModel::setMonthlyBudgetMb,
                    onMaxDurationChange = viewModel::setMaxDuration,
                    onCheckIntervalChange = viewModel::setCheckInterval,
                    onSignalDropToggle = viewModel::setTriggerOnSignalDrop,
                    onSignalDropThresholdChange = viewModel::setSignalDropThreshold,
                    onTechDowngradeToggle = viewModel::setTriggerOnTechDowngrade,
                    onDeadAirToggle = viewModel::setTriggerOnDeadAir,
                    onCompactTimelineModeToggle = viewModel::setCompactTimelineMode,
                    onMapAutoCenterToggle = viewModel::setMapAutoCenter,
                    onGlobalFontSizeChange = viewModel::setGlobalFontSize,
                    onStartHeavyTest = viewModel::requestManualSpeedTest,
                )
            }
        }
    }
}
