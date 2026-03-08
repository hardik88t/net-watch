package com.netwatch.app.core.model

data class ConnectionSnapshot(
    val timestampMs: Long,
    val profile: NetworkProfile,
    val technology: NetworkTechnology,
    val signalDbm: Int?,
    val hasInternet: Boolean,
    val isVpnActive: Boolean,
    val isProxyActive: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
)
