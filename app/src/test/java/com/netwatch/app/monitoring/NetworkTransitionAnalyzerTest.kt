package com.netwatch.app.monitoring

import com.google.common.truth.Truth.assertThat
import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.core.model.NetworkProfile
import com.netwatch.app.core.model.NetworkTechnology
import com.netwatch.app.core.model.ProfileType
import org.junit.Test

class NetworkTransitionAnalyzerTest {

    private val profile = NetworkProfile(
        key = "sim:1",
        displayName = "Carrier",
        type = ProfileType.SIM,
    )

    @Test
    fun `emits transition and outage start when network drops`() {
        val analyzer = NetworkTransitionAnalyzer()

        val previous = snapshot(
            timestamp = 1_000,
            technology = NetworkTechnology.NETWORK_5G,
            signal = -78,
        )
        val current = snapshot(
            timestamp = 2_000,
            technology = NetworkTechnology.NO_SERVICE,
            signal = null,
        )

        val events = analyzer.process(previous, current)

        assertThat(events).hasSize(2)
        assertThat(events[0].type).isEqualTo(NetworkEventType.TRANSITION)
        assertThat(events[1].type).isEqualTo(NetworkEventType.OUTAGE_START)
    }

    @Test
    fun `emits outage end with duration when service returns`() {
        val analyzer = NetworkTransitionAnalyzer()
        val firstDrop = analyzer.process(
            snapshot(1_000, NetworkTechnology.LTE, -86),
            snapshot(2_000, NetworkTechnology.NO_SERVICE, null),
        )
        assertThat(firstDrop).hasSize(2)

        val recoveryEvents = analyzer.process(
            snapshot(2_000, NetworkTechnology.NO_SERVICE, null),
            snapshot(9_000, NetworkTechnology.LTE, -91),
        )

        assertThat(recoveryEvents).hasSize(2)
        assertThat(recoveryEvents[1].type).isEqualTo(NetworkEventType.OUTAGE_END)
        assertThat(recoveryEvents[1].durationMs).isEqualTo(7_000)
    }

    private fun snapshot(
        timestamp: Long,
        technology: NetworkTechnology,
        signal: Int?,
    ): ConnectionSnapshot {
        return ConnectionSnapshot(
            timestampMs = timestamp,
            profile = profile,
            technology = technology,
            signalDbm = signal,
            hasInternet = !technology.isOutage(),
            isVpnActive = false,
            isProxyActive = false,
            latitude = null,
            longitude = null,
            totalRxBytes = 0,
            totalTxBytes = 0,
        )
    }
}
