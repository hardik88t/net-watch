package com.netwatch.app.data.repository

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkEvent
import com.netwatch.app.core.model.NetworkProfile
import com.netwatch.app.core.model.SpeedTestResult
import com.netwatch.app.core.model.TimelineItem
import com.netwatch.app.core.model.TimeScopedStats
import kotlinx.coroutines.flow.Flow

interface MonitoringRepository {
    suspend fun logSnapshot(snapshot: ConnectionSnapshot)
    suspend fun logEvent(event: NetworkEvent): Long
    suspend fun logSpeedTest(result: SpeedTestResult)
    suspend fun upsertProfile(profile: NetworkProfile, seenAtMs: Long)
    suspend fun addAnnotation(eventId: Long?, timestampMs: Long, text: String)
    suspend fun deleteAnnotation(eventId: Long)
    suspend fun setEventException(eventId: Long, isException: Boolean)
    suspend fun deleteEvent(eventId: Long)

    fun observeLatestSnapshot(): Flow<ConnectionSnapshot?>
    fun observeTimeline(limit: Int = 200, includeExceptions: Boolean = false): Flow<List<TimelineItem>>
    fun observeRecentSpeedTests(limit: Int = 10): Flow<List<SpeedTestResult>>
    suspend fun timeScopedStats(sinceMs: Long, nowMs: Long): TimeScopedStats
}
