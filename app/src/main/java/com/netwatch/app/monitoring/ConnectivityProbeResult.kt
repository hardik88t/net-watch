package com.netwatch.app.monitoring

data class ConnectivityProbeResult(
    val success: Boolean,
    val host: String,
    val latencyMs: Long,
    val checkedAtMs: Long,
    val error: String? = null,
)
