package com.netwatch.app.core.model

data class TimeScopedStats(
    val avgDownloadMbps: Double,
    val avgUploadMbps: Double,
    val avgLatencyMs: Double,
    val timeOnWifiMinutes: Long,
    val timeOn5gMinutes: Long,
    val timeOnLteMinutes: Long,
    val timeOnLegacyMinutes: Long,
    val switchFrequencyPerDay: Double,
)
