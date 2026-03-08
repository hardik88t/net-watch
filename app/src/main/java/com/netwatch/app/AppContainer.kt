package com.netwatch.app

import android.content.Context
import androidx.room.Room
import com.netwatch.app.data.local.NetWatchDatabase
import com.netwatch.app.data.repository.BudgetTracker
import com.netwatch.app.data.repository.DataStorePreferencesRepository
import com.netwatch.app.data.repository.PreferencesRepository
import com.netwatch.app.data.repository.RoomMonitoringRepository
import com.netwatch.app.export.LocalReportExporter
import com.netwatch.app.export.ReportExporter
import com.netwatch.app.monitoring.AndroidNetworkSnapshotProvider
import com.netwatch.app.monitoring.DeadAirAnomalyDetector
import com.netwatch.app.monitoring.HeavyTestTriggerEngine
import com.netwatch.app.monitoring.HttpLightweightConnectivityChecker
import com.netwatch.app.monitoring.HybridSpeedTestExecutor
import com.netwatch.app.monitoring.LightweightConnectivityChecker
import com.netwatch.app.monitoring.MonitorCoordinator
import com.netwatch.app.monitoring.NetworkSnapshotProvider
import com.netwatch.app.monitoring.NetworkTransitionAnalyzer
import com.netwatch.app.monitoring.SpeedTestExecutor

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val database: NetWatchDatabase = Room.databaseBuilder(
        appContext,
        NetWatchDatabase::class.java,
        "netwatch.db",
    ).fallbackToDestructiveMigration().build()

    val monitoringRepository = RoomMonitoringRepository(
        snapshotDao = database.stateSnapshotDao(),
        eventDao = database.networkEventDao(),
        speedTestDao = database.speedTestDao(),
        annotationDao = database.annotationDao(),
        profileDao = database.networkProfileDao(),
    )

    val preferencesRepository: PreferencesRepository = DataStorePreferencesRepository(appContext)
    val budgetTracker = BudgetTracker()

    val snapshotProvider: NetworkSnapshotProvider = AndroidNetworkSnapshotProvider(appContext)
    val connectivityChecker: LightweightConnectivityChecker = HttpLightweightConnectivityChecker()
    val speedTestExecutor: SpeedTestExecutor = HybridSpeedTestExecutor()

    private val transitionAnalyzer = NetworkTransitionAnalyzer()
    private val anomalyDetector = DeadAirAnomalyDetector()
    private val triggerEngine = HeavyTestTriggerEngine()

    val monitorCoordinator = MonitorCoordinator(
        repository = monitoringRepository,
        preferencesRepository = preferencesRepository,
        transitionAnalyzer = transitionAnalyzer,
        anomalyDetector = anomalyDetector,
        triggerEngine = triggerEngine,
        speedTestExecutor = speedTestExecutor,
        budgetTracker = budgetTracker,
    )

    val reportExporter: ReportExporter = LocalReportExporter(
        context = appContext,
        repository = monitoringRepository,
    )
}
