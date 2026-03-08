package com.netwatch.app.export

import android.content.Context
import com.netwatch.app.data.repository.MonitoringRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalReportExporter(
    private val context: Context,
    private val repository: MonitoringRepository,
) : ReportExporter {

    override suspend fun exportFormattedReport(): File = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val weekly = repository.weeklyStats(now)
        val timeline = repository.observeTimeline(limit = 200).first()

        val file = outputFile("report", "txt")
        val report = buildString {
            appendLine("NetWatch Formatted Report")
            appendLine("Generated: ${format(now)}")
            appendLine()
            appendLine("Summary")
            appendLine("- Avg Download: ${"%.2f".format(weekly.avgDownloadMbps)} Mbps")
            appendLine("- Avg Upload: ${"%.2f".format(weekly.avgUploadMbps)} Mbps")
            appendLine("- Avg Latency: ${"%.2f".format(weekly.avgLatencyMs)} ms")
            appendLine("- 5G Minutes: ${weekly.timeOn5gMinutes}")
            appendLine("- LTE Minutes: ${weekly.timeOnLteMinutes}")
            appendLine("- Legacy Minutes: ${weekly.timeOnLegacyMinutes}")
            appendLine("- Switch/day: ${"%.2f".format(weekly.switchFrequencyPerDay)}")
            appendLine()
            appendLine("Timeline")
            timeline.forEach { item ->
                appendLine("${format(item.event.timestampMs)} | ${item.event.type} | ${item.event.message}")
                item.note?.let { note -> appendLine("  Note: $note") }
            }
        }

        file.writeText(report)
        file
    }

    override suspend fun exportRawCsv(): File = withContext(Dispatchers.IO) {
        val timeline = repository.observeTimeline(limit = 2000).first()
        val file = outputFile("timeline", "csv")
        val csv = buildString {
            appendLine("timestamp,event_type,profile,from,to,signal_dbm,message,lat,lng,duration_ms,note")
            timeline.forEach { item ->
                val event = item.event
                appendLine(
                    listOf(
                        event.timestampMs,
                        event.type,
                        event.profileKey.orEmpty(),
                        event.previousTechnology?.name.orEmpty(),
                        event.currentTechnology?.name.orEmpty(),
                        event.signalDbm ?: "",
                        csvEscape(event.message),
                        event.latitude ?: "",
                        event.longitude ?: "",
                        event.durationMs ?: "",
                        csvEscape(item.note.orEmpty()),
                    ).joinToString(",")
                )
            }
        }
        file.writeText(csv)
        file
    }

    override suspend fun exportRawJson(): File = withContext(Dispatchers.IO) {
        val timeline = repository.observeTimeline(limit = 2000).first().map { item ->
            JsonTimelineItem(
                timestampMs = item.event.timestampMs,
                type = item.event.type.name,
                profileKey = item.event.profileKey,
                previousTechnology = item.event.previousTechnology?.name,
                currentTechnology = item.event.currentTechnology?.name,
                signalDbm = item.event.signalDbm,
                message = item.event.message,
                latitude = item.event.latitude,
                longitude = item.event.longitude,
                durationMs = item.event.durationMs,
                note = item.note,
            )
        }

        val file = outputFile("timeline", "json")
        file.writeText(Json { prettyPrint = true }.encodeToString(timeline))
        file
    }

    private fun outputFile(prefix: String, ext: String): File {
        return File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
    }

    private fun format(timestampMs: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return formatter.format(Date(timestampMs))
    }

    private fun csvEscape(input: String): String {
        val escaped = input.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}

@Serializable
private data class JsonTimelineItem(
    val timestampMs: Long,
    val type: String,
    val profileKey: String?,
    val previousTechnology: String?,
    val currentTechnology: String?,
    val signalDbm: Int?,
    val message: String,
    val latitude: Double?,
    val longitude: Double?,
    val durationMs: Long?,
    val note: String?,
)
