package com.netwatch.app.monitoring

interface LightweightConnectivityChecker {
    suspend fun check(): ConnectivityProbeResult
}
