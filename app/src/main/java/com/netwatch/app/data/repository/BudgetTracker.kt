package com.netwatch.app.data.repository

import java.time.Instant
import java.time.ZoneOffset

class BudgetTracker {
    private var dayKey: String = ""
    private var monthKey: String = ""
    private var todayBytes: Long = 0
    private var monthBytes: Long = 0

    fun snapshot(nowMs: Long): UsageSnapshot {
        rollOver(nowMs)
        return UsageSnapshot(todayBytes = todayBytes, monthBytes = monthBytes)
    }

    fun record(bytes: Long, nowMs: Long) {
        rollOver(nowMs)
        todayBytes += bytes
        monthBytes += bytes
    }

    private fun rollOver(nowMs: Long) {
        val instant = Instant.ofEpochMilli(nowMs).atZone(ZoneOffset.UTC)
        val newDay = "${instant.year}-${instant.dayOfYear}"
        val newMonth = "${instant.year}-${instant.monthValue}"
        if (newDay != dayKey) {
            dayKey = newDay
            todayBytes = 0
        }
        if (newMonth != monthKey) {
            monthKey = newMonth
            monthBytes = 0
        }
    }
}

data class UsageSnapshot(
    val todayBytes: Long,
    val monthBytes: Long,
)
