package com.netwatch.app.monitoring

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkEvent
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.data.repository.BudgetTracker
import com.netwatch.app.data.repository.MonitoringRepository
import com.netwatch.app.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MonitorCoordinator(
    private val repository: MonitoringRepository,
    private val preferencesRepository: PreferencesRepository,
    private val transitionAnalyzer: NetworkTransitionAnalyzer,
    private val anomalyDetector: DeadAirAnomalyDetector,
    private val triggerEngine: HeavyTestTriggerEngine,
    private val speedTestExecutor: SpeedTestExecutor,
    private val budgetTracker: BudgetTracker,
) {

    private var previousSnapshot: ConnectionSnapshot? = null
    private val heavyTestMutex = Mutex()

    suspend fun processSnapshot(snapshot: ConnectionSnapshot) {
        repository.upsertProfile(snapshot.profile, snapshot.timestampMs)
        repository.logSnapshot(snapshot)

        val events = transitionAnalyzer.process(previousSnapshot, snapshot)
        for (event in events) {
            repository.logEvent(event)
            maybeRunHeavyTest(event, previousSnapshot, snapshot)
        }

        previousSnapshot = snapshot
    }

    suspend fun processProbe(snapshot: ConnectionSnapshot, probe: ConnectivityProbeResult) {
        val anomaly = anomalyDetector.detect(snapshot, probe) ?: return
        repository.logEvent(anomaly)
        maybeRunHeavyTest(anomaly, previousSnapshot, snapshot)
    }

    suspend fun runManualHeavyTest(snapshot: ConnectionSnapshot) {
        val constraints = preferencesRepository.constraints.first()
        val decision = triggerEngine.evaluateManual(constraints)
        if (!decision.shouldRun) {
            repository.logEvent(
                NetworkEvent(
                    timestampMs = System.currentTimeMillis(),
                    type = NetworkEventType.SPEED_TEST,
                    profileKey = snapshot.profile.key,
                    previousTechnology = snapshot.technology,
                    currentTechnology = snapshot.technology,
                    signalDbm = snapshot.signalDbm,
                    message = "Manual heavy test blocked: ${decision.blockedReason}",
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude,
                )
            )
            return
        }

        runHeavyTest(reason = decision.reason ?: "Manual heavy test", snapshot = snapshot)
    }

    private suspend fun maybeRunHeavyTest(
        event: NetworkEvent,
        previous: ConnectionSnapshot?,
        current: ConnectionSnapshot,
    ) {
        val constraints = preferencesRepository.constraints.first()
        val usage = budgetTracker.snapshot(current.timestampMs)
        val decision = triggerEngine.evaluate(
            event = event,
            previous = previous,
            current = current,
            constraints = constraints,
            usage = usage,
        )

        if (!decision.shouldRun) {
            return
        }

        runHeavyTest(reason = decision.reason ?: "Auto heavy test", snapshot = current)
    }

    private suspend fun runHeavyTest(reason: String, snapshot: ConnectionSnapshot) {
        heavyTestMutex.withLock {
            val result = speedTestExecutor.run(reason, snapshot)
            repository.logSpeedTest(result)
            budgetTracker.record(result.bytesConsumed, result.timestampMs)
            repository.logEvent(
                NetworkEvent(
                    timestampMs = result.timestampMs,
                    type = NetworkEventType.SPEED_TEST,
                    profileKey = snapshot.profile.key,
                    previousTechnology = snapshot.technology,
                    currentTechnology = snapshot.technology,
                    signalDbm = snapshot.signalDbm,
                    message = "Speed test: ${"%.1f".format(result.downloadMbps)}↓ ${"%.1f".format(result.uploadMbps)}↑ ${"%.1f".format(result.latencyMs)}ms",
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude,
                )
            )
        }
    }
}
