package com.netwatch.app.monitoring

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkEvent
import com.netwatch.app.core.model.NetworkEventType

class DeadAirAnomalyDetector(
    private val strongSignalDbmThreshold: Int = -95,
    private val cooldownMs: Long = 2 * 60 * 1000,
) {
    private var lastEmissionMs: Long = 0

    fun detect(snapshot: ConnectionSnapshot, probe: ConnectivityProbeResult): NetworkEvent? {
        val strongSignal = snapshot.signalDbm?.let { it >= strongSignalDbmThreshold } ?: false
        val notOutage = !snapshot.technology.isOutage()
        val enoughTimeElapsed = lastEmissionMs == 0L || (probe.checkedAtMs - lastEmissionMs >= cooldownMs)

        if (!probe.success && strongSignal && notOutage && enoughTimeElapsed) {
            lastEmissionMs = probe.checkedAtMs
            return NetworkEvent(
                timestampMs = probe.checkedAtMs,
                type = NetworkEventType.ANOMALY,
                profileKey = snapshot.profile.key,
                previousTechnology = snapshot.technology,
                currentTechnology = snapshot.technology,
                signalDbm = snapshot.signalDbm,
                message = "Dead air anomaly: strong signal but probe failed (${probe.error ?: "unknown error"})",
                latitude = snapshot.latitude,
                longitude = snapshot.longitude,
            )
        }

        return null
    }
}
