package com.netwatch.app.monitoring

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkEvent
import com.netwatch.app.core.model.NetworkEventType

class NetworkTransitionAnalyzer {
    private var outageStartedAtMs: Long? = null

    fun process(previous: ConnectionSnapshot?, current: ConnectionSnapshot): List<NetworkEvent> {
        if (previous == null) {
            if (current.technology.isOutage()) {
                outageStartedAtMs = current.timestampMs
                return listOf(
                    NetworkEvent(
                        timestampMs = current.timestampMs,
                        type = NetworkEventType.OUTAGE_START,
                        profileKey = current.profile.key,
                        previousTechnology = null,
                        currentTechnology = current.technology,
                        signalDbm = current.signalDbm,
                        message = "Absolute Zero started (${current.technology})",
                        latitude = current.latitude,
                        longitude = current.longitude,
                    )
                )
            }
            return emptyList()
        }

        if (previous.technology == current.technology) {
            return emptyList()
        }

        val events = mutableListOf<NetworkEvent>()
        events += NetworkEvent(
            timestampMs = current.timestampMs,
            type = NetworkEventType.TRANSITION,
            profileKey = current.profile.key,
            previousTechnology = previous.technology,
            currentTechnology = current.technology,
            signalDbm = current.signalDbm,
            message = "${previous.technology} -> ${current.technology}",
            latitude = current.latitude,
            longitude = current.longitude,
        )

        if (!previous.technology.isOutage() && current.technology.isOutage()) {
            outageStartedAtMs = current.timestampMs
            events += NetworkEvent(
                timestampMs = current.timestampMs,
                type = NetworkEventType.OUTAGE_START,
                profileKey = current.profile.key,
                previousTechnology = previous.technology,
                currentTechnology = current.technology,
                signalDbm = current.signalDbm,
                message = "Absolute Zero started (${current.technology})",
                latitude = current.latitude,
                longitude = current.longitude,
            )
        }

        if (previous.technology.isOutage() && !current.technology.isOutage()) {
            val duration = outageStartedAtMs?.let { current.timestampMs - it }
            outageStartedAtMs = null
            events += NetworkEvent(
                timestampMs = current.timestampMs,
                type = NetworkEventType.OUTAGE_END,
                profileKey = current.profile.key,
                previousTechnology = previous.technology,
                currentTechnology = current.technology,
                signalDbm = current.signalDbm,
                message = "Absolute Zero ended (${current.technology})",
                latitude = current.latitude,
                longitude = current.longitude,
                durationMs = duration,
            )
        }

        return events
    }
}
