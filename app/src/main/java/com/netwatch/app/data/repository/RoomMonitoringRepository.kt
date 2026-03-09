package com.netwatch.app.data.repository

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkEvent
import com.netwatch.app.core.model.NetworkProfile
import com.netwatch.app.core.model.ProfileType
import com.netwatch.app.core.model.SpeedTestResult
import com.netwatch.app.core.model.TimelineItem
import com.netwatch.app.core.model.TimeScopedStats
import com.netwatch.app.core.model.NetworkTechnology
import com.netwatch.app.data.local.dao.AnnotationDao
import com.netwatch.app.data.local.dao.NetworkEventDao
import com.netwatch.app.data.local.dao.NetworkProfileDao
import com.netwatch.app.data.local.dao.SpeedTestDao
import com.netwatch.app.data.local.dao.StateSnapshotDao
import com.netwatch.app.data.local.entity.AnnotationEntity
import com.netwatch.app.data.local.entity.NetworkEventEntity
import com.netwatch.app.data.local.entity.NetworkProfileEntity
import com.netwatch.app.data.local.entity.SpeedTestResultEntity
import com.netwatch.app.data.local.entity.StateSnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomMonitoringRepository(
    private val snapshotDao: StateSnapshotDao,
    private val eventDao: NetworkEventDao,
    private val speedTestDao: SpeedTestDao,
    private val annotationDao: AnnotationDao,
    private val profileDao: NetworkProfileDao,
) : MonitoringRepository {

    private var lastSnapshotMs = 0L
    private var lastEventMs = 0L
    private var lastSpeedTestMs = 0L

    private fun validateTimestamp(
        timestampMs: Long,
        lastSeenMs: Long,
        updateLastSeen: (Long) -> Unit
    ): Boolean {
        if (timestampMs <= 0) return false
        if (timestampMs < lastSeenMs) return false
        val now = System.currentTimeMillis()
        if (timestampMs > now + 86400000L) return false

        updateLastSeen(timestampMs)
        return true
    }

    private fun validateLatLng(lat: Double?, lng: Double?): Boolean {
        if (lat == null && lng == null) return true
        if (lat == null || lng == null) return false
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    private fun validateSignal(dbm: Int?): Boolean {
        if (dbm == null) return true
        return dbm in -150..0
    }

    override suspend fun logSnapshot(snapshot: ConnectionSnapshot) {
        if (snapshot.profile.key.isBlank()) return
        if (!validateTimestamp(snapshot.timestampMs, lastSnapshotMs) { lastSnapshotMs = it }) return
        if (!validateLatLng(snapshot.latitude, snapshot.longitude)) return
        if (!validateSignal(snapshot.signalDbm)) return

        snapshotDao.insert(
            StateSnapshotEntity(
                timestampMs = snapshot.timestampMs,
                profileKey = snapshot.profile.key,
                profileLabel = snapshot.profile.displayName,
                profileType = snapshot.profile.type.name,
                technology = snapshot.technology,
                signalDbm = snapshot.signalDbm,
                hasInternet = snapshot.hasInternet,
                isVpnActive = snapshot.isVpnActive,
                isProxyActive = snapshot.isProxyActive,
                latitude = snapshot.latitude,
                longitude = snapshot.longitude,
                totalRxBytes = snapshot.totalRxBytes,
                totalTxBytes = snapshot.totalTxBytes,
            )
        )
    }

    override suspend fun logEvent(event: NetworkEvent): Long {
        if (event.profileKey.isNullOrBlank()) return -1L
        if (!validateTimestamp(event.timestampMs, lastEventMs) { lastEventMs = it }) return -1L
        if (!validateLatLng(event.latitude, event.longitude)) return -1L
        if (!validateSignal(event.signalDbm)) return -1L

        return eventDao.insert(
            NetworkEventEntity(
                timestampMs = event.timestampMs,
                type = event.type,
                profileKey = event.profileKey,
                previousTechnology = event.previousTechnology,
                currentTechnology = event.currentTechnology,
                signalDbm = event.signalDbm,
                message = event.message,
                latitude = event.latitude,
                longitude = event.longitude,
                durationMs = event.durationMs,
                isException = false
            )
        )
    }

    override suspend fun logSpeedTest(result: SpeedTestResult) {
        if (result.profileKey.isNullOrBlank()) return
        if (!validateTimestamp(result.timestampMs, lastSpeedTestMs) { lastSpeedTestMs = it }) return
        if (!validateLatLng(result.latitude, result.longitude)) return

        speedTestDao.insert(
            SpeedTestResultEntity(
                timestampMs = result.timestampMs,
                triggerReason = result.triggerReason,
                profileKey = result.profileKey,
                downloadMbps = result.downloadMbps,
                uploadMbps = result.uploadMbps,
                latencyMs = result.latencyMs,
                isVpnActive = result.isVpnActive,
                isProxyActive = result.isProxyActive,
                latitude = result.latitude,
                longitude = result.longitude,
                bytesConsumed = result.bytesConsumed,
                estimated = result.estimated,
            )
        )
    }

    override suspend fun upsertProfile(profile: NetworkProfile, seenAtMs: Long) {
        if (profile.key.isBlank()) return

        profileDao.upsert(
            NetworkProfileEntity(
                key = profile.key,
                displayName = profile.displayName,
                type = profile.type.name,
                carrierName = profile.carrierName,
                simSlotIndex = profile.simSlotIndex,
                subscriptionId = profile.subscriptionId,
                ssid = profile.ssid,
                bssid = profile.bssid,
                lastSeenAtMs = seenAtMs,
            )
        )
    }

    override suspend fun addAnnotation(eventId: Long?, timestampMs: Long, text: String) {
        if (eventId != null) {
            annotationDao.deleteForEvent(eventId)
        }
        annotationDao.insert(
            AnnotationEntity(
                eventId = eventId,
                timestampMs = timestampMs,
                text = text,
            )
        )
    }

    override suspend fun deleteAnnotation(eventId: Long) {
        annotationDao.deleteForEvent(eventId)
    }

    override suspend fun setEventException(eventId: Long, isException: Boolean) {
        eventDao.setException(eventId, isException)
    }

    override suspend fun deleteEvent(eventId: Long) {
        eventDao.deleteById(eventId)
    }

    override fun observeLatestSnapshot(): Flow<ConnectionSnapshot?> {
        return snapshotDao.observeLatest().map { entity ->
            entity?.toModel()
        }
    }

    override fun observeTimeline(limit: Int, includeExceptions: Boolean): Flow<List<TimelineItem>> {
        val eventsFlow = if (includeExceptions) {
            eventDao.observeTimelineAll(limit)
        } else {
            eventDao.observeTimeline(limit)
        }
        return combine(
            eventsFlow,
            annotationDao.observeAll(),
        ) { events, annotations ->
            val notesByEvent = annotations.associateBy { it.eventId }
            events.map { eventEntity ->
                val note = notesByEvent[eventEntity.id]?.text
                TimelineItem(event = eventEntity.toModel(), note = note)
            }
        }
    }

    override fun observeRecentSpeedTests(limit: Int): Flow<List<SpeedTestResult>> {
        return speedTestDao.observeRecent(limit).map { list ->
            list.map { it.toModel() }
        }
    }

    override suspend fun timeScopedStats(sinceMs: Long, nowMs: Long): TimeScopedStats {
        val snapshots = snapshotDao.getSince(sinceMs)
        var timeOnWifiMs = 0L
        var timeOn5gMs = 0L
        var timeOnLteMs = 0L
        var timeOnLegacyMs = 0L
        var switches = 0

        val avgDownload = speedTestDao.averageDownloadSince(sinceMs)
        val avgUpload = speedTestDao.averageUploadSince(sinceMs)
        val avgLatency = speedTestDao.averageLatencySince(sinceMs)

        if (snapshots.isNotEmpty()) {
            snapshots.forEachIndexed { index, snapshot ->
                val nextTimestamp = snapshots.getOrNull(index + 1)?.timestampMs ?: nowMs
                val durationMs = (nextTimestamp - snapshot.timestampMs).coerceAtLeast(0)
                when (snapshot.technology) {
                    NetworkTechnology.WIFI -> timeOnWifiMs += durationMs
                    NetworkTechnology.NETWORK_5G -> timeOn5gMs += durationMs
                    NetworkTechnology.LTE -> timeOnLteMs += durationMs
                    NetworkTechnology.NETWORK_2G, NetworkTechnology.NETWORK_3G -> timeOnLegacyMs += durationMs
                    else -> Unit
                }

                val next = snapshots.getOrNull(index + 1)
                if (next != null && next.technology != snapshot.technology) {
                    switches += 1
                }
            }
        }

        val days = ((nowMs - sinceMs) / (1000.0 * 60 * 60 * 24)).coerceAtLeast(1.0)

        return TimeScopedStats(
            avgDownloadMbps = avgDownload,
            avgUploadMbps = avgUpload,
            avgLatencyMs = avgLatency,
            timeOnWifiMinutes = timeOnWifiMs / 60_000,
            timeOn5gMinutes = timeOn5gMs / 60_000,
            timeOnLteMinutes = timeOnLteMs / 60_000,
            timeOnLegacyMinutes = timeOnLegacyMs / 60_000,
            switchFrequencyPerDay = switches / days,
        )
    }

    private fun StateSnapshotEntity.toModel(): ConnectionSnapshot {
        return ConnectionSnapshot(
            timestampMs = timestampMs,
            profile = NetworkProfile(
                key = profileKey,
                displayName = profileLabel,
                type = runCatching { ProfileType.valueOf(profileType) }.getOrDefault(ProfileType.UNKNOWN),
            ),
            technology = technology,
            signalDbm = signalDbm,
            hasInternet = hasInternet,
            isVpnActive = isVpnActive,
            isProxyActive = isProxyActive,
            latitude = latitude,
            longitude = longitude,
            totalRxBytes = totalRxBytes,
            totalTxBytes = totalTxBytes,
        )
    }

    private fun NetworkEventEntity.toModel(): NetworkEvent {
        return NetworkEvent(
            id = id,
            timestampMs = timestampMs,
            type = type,
            profileKey = profileKey,
            previousTechnology = previousTechnology,
            currentTechnology = currentTechnology,
            signalDbm = signalDbm,
            message = message,
            latitude = latitude,
            longitude = longitude,
            durationMs = durationMs,
        )
    }

    private fun SpeedTestResultEntity.toModel(): SpeedTestResult {
        return SpeedTestResult(
            id = id,
            timestampMs = timestampMs,
            triggerReason = triggerReason,
            profileKey = profileKey,
            downloadMbps = downloadMbps,
            uploadMbps = uploadMbps,
            latencyMs = latencyMs,
            isVpnActive = isVpnActive,
            isProxyActive = isProxyActive,
            latitude = latitude,
            longitude = longitude,
            bytesConsumed = bytesConsumed,
            estimated = estimated,
        )
    }
}
