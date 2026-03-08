package com.netwatch.app.data.repository

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkEvent
import com.netwatch.app.core.model.NetworkProfile
import com.netwatch.app.core.model.ProfileType
import com.netwatch.app.core.model.SpeedTestResult
import com.netwatch.app.core.model.TimelineItem
import com.netwatch.app.core.model.WeeklyStats
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

    override suspend fun logSnapshot(snapshot: ConnectionSnapshot) {
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
            )
        )
    }

    override suspend fun logSpeedTest(result: SpeedTestResult) {
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
        annotationDao.insert(
            AnnotationEntity(
                eventId = eventId,
                timestampMs = timestampMs,
                text = text,
            )
        )
    }

    override fun observeLatestSnapshot(): Flow<ConnectionSnapshot?> {
        return snapshotDao.observeLatest().map { entity ->
            entity?.toModel()
        }
    }

    override fun observeTimeline(limit: Int): Flow<List<TimelineItem>> {
        return combine(
            eventDao.observeTimeline(limit),
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

    override suspend fun weeklyStats(nowMs: Long): WeeklyStats {
        val weekMs = 7L * 24 * 60 * 60 * 1000
        val since = nowMs - weekMs

        val snapshots = snapshotDao.getSince(since)
        var timeOn5g = 0L
        var timeOnLte = 0L
        var timeOnLegacy = 0L
        var switches = 0

        val avgDownload = speedTestDao.averageDownloadSince(since)
        val avgUpload = speedTestDao.averageUploadSince(since)
        val avgLatency = speedTestDao.averageLatencySince(since)

        if (snapshots.isNotEmpty()) {
            snapshots.forEachIndexed { index, snapshot ->
                val nextTimestamp = snapshots.getOrNull(index + 1)?.timestampMs ?: nowMs
                val durationMinutes = ((nextTimestamp - snapshot.timestampMs).coerceAtLeast(0) / 60_000)
                when (snapshot.technology) {
                    NetworkTechnology.NETWORK_5G -> timeOn5g += durationMinutes
                    NetworkTechnology.LTE -> timeOnLte += durationMinutes
                    NetworkTechnology.NETWORK_2G, NetworkTechnology.NETWORK_3G -> timeOnLegacy += durationMinutes
                    else -> Unit
                }

                val next = snapshots.getOrNull(index + 1)
                if (next != null && next.technology != snapshot.technology) {
                    switches += 1
                }
            }
        }

        return WeeklyStats(
            avgDownloadMbps = avgDownload,
            avgUploadMbps = avgUpload,
            avgLatencyMs = avgLatency,
            timeOn5gMinutes = timeOn5g,
            timeOnLteMinutes = timeOnLte,
            timeOnLegacyMinutes = timeOnLegacy,
            switchFrequencyPerDay = switches / 7.0,
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
