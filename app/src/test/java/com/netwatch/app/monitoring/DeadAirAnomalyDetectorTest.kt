package com.netwatch.app.monitoring

import com.google.common.truth.Truth.assertThat
import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.core.model.NetworkProfile
import com.netwatch.app.core.model.NetworkTechnology
import com.netwatch.app.core.model.ProfileType
import org.junit.Test

class DeadAirAnomalyDetectorTest {

    private val profile = NetworkProfile(
        key = "wifi:ap1",
        displayName = "Office AP",
        type = ProfileType.WIFI,
    )

    @Test
    fun `detects dead air when probe fails under strong signal`() {
        val detector = DeadAirAnomalyDetector(strongSignalDbmThreshold = -95)

        val anomaly = detector.detect(
            snapshot(signal = -70),
            ConnectivityProbeResult(
                success = false,
                host = "test",
                latencyMs = 100,
                checkedAtMs = 5_000,
                error = "timeout",
            )
        )

        assertThat(anomaly).isNotNull()
        assertThat(anomaly?.type).isEqualTo(NetworkEventType.ANOMALY)
        assertThat(anomaly?.message).contains("Dead air")
    }

    @Test
    fun `does not detect dead air when signal is weak`() {
        val detector = DeadAirAnomalyDetector(strongSignalDbmThreshold = -95)

        val anomaly = detector.detect(
            snapshot(signal = -108),
            ConnectivityProbeResult(
                success = false,
                host = "test",
                latencyMs = 150,
                checkedAtMs = 7_000,
                error = "timeout",
            )
        )

        assertThat(anomaly).isNull()
    }

    private fun snapshot(signal: Int): ConnectionSnapshot {
        return ConnectionSnapshot(
            timestampMs = 1_000,
            profile = profile,
            technology = NetworkTechnology.WIFI,
            signalDbm = signal,
            hasInternet = true,
            isVpnActive = false,
            isProxyActive = false,
            latitude = 0.0,
            longitude = 0.0,
            totalRxBytes = 0,
            totalTxBytes = 0,
        )
    }
}
