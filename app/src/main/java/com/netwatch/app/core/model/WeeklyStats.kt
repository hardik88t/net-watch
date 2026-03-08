package com.netwatch.app.core.model

data class WeeklyStats(
    val avgDownloadMbps: Double,
    val avgUploadMbps: Double,
    val avgLatencyMs: Double,
    val timeOn5gMinutes: Long,
    val timeOnLteMinutes: Long,
    val timeOnLegacyMinutes: Long,
    val switchFrequencyPerDay: Double,
)
