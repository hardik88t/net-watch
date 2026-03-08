package com.netwatch.app.monitoring

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.MonitoringConstraints
import com.netwatch.app.core.model.NetworkEvent
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.data.repository.UsageSnapshot

class HeavyTestTriggerEngine(
    private val estimatedHeavyTestBytes: Long = 5L * 1024L * 1024L,
) {

    fun evaluate(
        event: NetworkEvent,
        previous: ConnectionSnapshot?,
        current: ConnectionSnapshot,
        constraints: MonitoringConstraints,
        usage: UsageSnapshot,
    ): TriggerDecision {
        if (!constraints.monitoringEnabled) {
            return TriggerDecision(false, blockedReason = "Monitoring is disabled")
        }

        if (wouldExceedBudget(constraints, usage)) {
            return TriggerDecision(false, blockedReason = "Configured data budget reached")
        }

        if (event.type == NetworkEventType.ANOMALY && constraints.triggerOnDeadAir) {
            return TriggerDecision(true, reason = "Auto heavy test: dead-air anomaly")
        }

        if (event.type == NetworkEventType.TRANSITION) {
            val previousTech = previous?.technology
            val currentTech = current.technology

            val downgradeTriggered = previousTech != null &&
                constraints.triggerOnTechDowngrade &&
                previousTech.rank > currentTech.rank

            if (downgradeTriggered) {
                return TriggerDecision(true, reason = "Auto heavy test: technology downgrade")
            }

            val signalDropTriggered = constraints.triggerOnSignalDrop &&
                previous?.signalDbm != null &&
                current.signalDbm != null &&
                previous.signalDbm - current.signalDbm >= constraints.signalDropThresholdDbm

            if (signalDropTriggered) {
                return TriggerDecision(true, reason = "Auto heavy test: sudden signal drop")
            }
        }

        return TriggerDecision(false)
    }

    fun evaluateManual(constraints: MonitoringConstraints): TriggerDecision {
        return if (constraints.monitoringEnabled) {
            TriggerDecision(true, reason = "Manual heavy test")
        } else {
            TriggerDecision(false, blockedReason = "Monitoring is disabled")
        }
    }

    private fun wouldExceedBudget(
        constraints: MonitoringConstraints,
        usage: UsageSnapshot,
    ): Boolean {
        val dailyBudget = constraints.dailyBudgetMb * 1024L * 1024L
        val monthlyBudget = constraints.monthlyBudgetMb * 1024L * 1024L

        val exceedsDaily = usage.todayBytes + estimatedHeavyTestBytes > dailyBudget
        val exceedsMonthly = usage.monthBytes + estimatedHeavyTestBytes > monthlyBudget
        return exceedsDaily || exceedsMonthly
    }
}

data class TriggerDecision(
    val shouldRun: Boolean,
    val reason: String? = null,
    val blockedReason: String? = null,
)
