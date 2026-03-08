package com.netwatch.app.core.model

data class SpeedTestResult(
    val id: Long = 0,
    val timestampMs: Long,
    val triggerReason: String,
    val profileKey: String?,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Double,
    val isVpnActive: Boolean,
    val isProxyActive: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val bytesConsumed: Long,
    val estimated: Boolean,
)
