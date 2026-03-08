package com.netwatch.app.core.model

enum class NetworkEventType {
    TRANSITION,
    OUTAGE_START,
    OUTAGE_END,
    ANOMALY,
    SPEED_TEST,
    MANUAL_NOTE
}

data class NetworkEvent(
    val id: Long = 0,
    val timestampMs: Long,
    val type: NetworkEventType,
    val profileKey: String?,
    val previousTechnology: NetworkTechnology?,
    val currentTechnology: NetworkTechnology?,
    val signalDbm: Int?,
    val message: String,
    val latitude: Double?,
    val longitude: Double?,
    val durationMs: Long? = null,
)
