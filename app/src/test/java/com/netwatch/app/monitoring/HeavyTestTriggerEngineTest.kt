package com.netwatch.app.monitoring

import com.google.common.truth.Truth.assertThat
import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.MonitoringConstraints
import com.netwatch.app.core.model.NetworkEvent
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.core.model.NetworkProfile
import com.netwatch.app.core.model.NetworkTechnology
import com.netwatch.app.core.model.ProfileType
import com.netwatch.app.data.repository.UsageSnapshot
import org.junit.Test

class HeavyTestTriggerEngineTest {

    private val profile = NetworkProfile(
        key = "sim:2",
        displayName = "Carrier B",
        type = ProfileType.SIM,
    )

    @Test
    fun `triggers heavy test on technology downgrade`() {
        val engine = HeavyTestTriggerEngine(estimatedHeavyTestBytes = 1_024)
        val constraints = MonitoringConstraints(triggerOnTechDowngrade = true)
        val previous = snapshot(NetworkTechnology.NETWORK_5G, -78)
        val current = snapshot(NetworkTechnology.LTE, -84)
        val event = event(previous.technology, current.technology)

        val decision = engine.evaluate(
            event = event,
            previous = previous,
            current = current,
            constraints = constraints,
            usage = UsageSnapshot(todayBytes = 0, monthBytes = 0),
        )

        assertThat(decision.shouldRun).isTrue()
        assertThat(decision.reason).contains("downgrade")
    }

    @Test
    fun `blocks heavy test when budget exceeded`() {
        val engine = HeavyTestTriggerEngine(estimatedHeavyTestBytes = 5 * 1024 * 1024)
        val constraints = MonitoringConstraints(
            dailyBudgetMb = 1,
            monthlyBudgetMb = 1,
            triggerOnDeadAir = true,
        )

        val decision = engine.evaluate(
            event = NetworkEvent(
                timestampMs = 1_000,
                type = NetworkEventType.ANOMALY,
                profileKey = profile.key,
                previousTechnology = NetworkTechnology.LTE,
                currentTechnology = NetworkTechnology.LTE,
                signalDbm = -70,
                message = "Dead air",
                latitude = null,
                longitude = null,
            ),
            previous = snapshot(NetworkTechnology.LTE, -70),
            current = snapshot(NetworkTechnology.LTE, -70),
            constraints = constraints,
            usage = UsageSnapshot(todayBytes = 1_048_000, monthBytes = 1_048_000),
        )

        assertThat(decision.shouldRun).isFalse()
        assertThat(decision.blockedReason).contains("budget")
    }

    private fun snapshot(technology: NetworkTechnology, signal: Int): ConnectionSnapshot {
        return ConnectionSnapshot(
            timestampMs = 1_000,
            profile = profile,
            technology = technology,
            signalDbm = signal,
            hasInternet = true,
            isVpnActive = false,
            isProxyActive = false,
            latitude = null,
            longitude = null,
            totalRxBytes = 0,
            totalTxBytes = 0,
        )
    }

    private fun event(from: NetworkTechnology, to: NetworkTechnology): NetworkEvent {
        return NetworkEvent(
            timestampMs = 2_000,
            type = NetworkEventType.TRANSITION,
            profileKey = profile.key,
            previousTechnology = from,
            currentTechnology = to,
            signalDbm = -84,
            message = "$from -> $to",
            latitude = null,
            longitude = null,
        )
    }
}
