package com.netwatch.app.export

import android.content.Context
import com.netwatch.app.data.repository.MonitoringRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalReportExporter(
    private val context: Context,
    private val repository: MonitoringRepository,
) : ReportExporter {

    override suspend fun exportFormattedReport(): File = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val stats = repository.timeScopedStats(now - 7L * 24 * 60 * 60 * 1000, now)
        val timeline = repository.observeTimeline(limit = 200).first()

        val file = outputFile("report", "txt")
        val report = buildString {
            appendLine("NetWatch Formatted Report")
            appendLine("Generated: ${format(now)}")
            appendLine()
            appendLine("Summary")
            appendLine("- Avg Download: ${"%.2f".format(stats.avgDownloadMbps)} Mbps")
            appendLine("- Avg Upload: ${"%.2f".format(stats.avgUploadMbps)} Mbps")
            appendLine("- Avg Latency: ${"%.2f".format(stats.avgLatencyMs)} ms")
            appendLine("- Wi-Fi Minutes: ${stats.timeOnWifiMinutes}")
            appendLine("- 5G Minutes: ${stats.timeOn5gMinutes}")
            appendLine("- LTE Minutes: ${stats.timeOnLteMinutes}")
            appendLine("- Legacy Minutes: ${stats.timeOnLegacyMinutes}")
            appendLine("- Switch/day: ${"%.2f".format(stats.switchFrequencyPerDay)}")
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

    override suspend fun exportPdfReport(): File = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val stats = repository.timeScopedStats(now - 7L * 24 * 60 * 60 * 1000, now)
        val timeline = repository.observeTimeline(limit = 200).first()

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 width, height, pageNum
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
            color = android.graphics.Color.BLACK
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = android.graphics.Color.DKGRAY
        }
        val textPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 12f
            color = android.graphics.Color.BLACK
        }

        var yPos = 50f
        val xMargin = 50f
        val yMarginBot = 800f

        fun checkPage() {
            if (yPos > yMarginBot) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
            }
        }

        canvas.drawText("NetWatch Weekly Report", xMargin, yPos, titlePaint)
        yPos += 30f
        canvas.drawText("Generated: ${format(now)}", xMargin, yPos, textPaint)
        yPos += 40f

        canvas.drawText("Summary", xMargin, yPos, headerPaint)
        yPos += 20f
        canvas.drawText("- Avg Download: ${"%.2f".format(stats.avgDownloadMbps)} Mbps", xMargin + 10, yPos, textPaint)
        yPos += 20f
        canvas.drawText("- Avg Upload: ${"%.2f".format(stats.avgUploadMbps)} Mbps", xMargin + 10, yPos, textPaint)
        yPos += 20f
        canvas.drawText("- Avg Latency: ${"%.2f".format(stats.avgLatencyMs)} ms", xMargin + 10, yPos, textPaint)
        yPos += 20f
        canvas.drawText("- Wi-Fi Minutes: ${stats.timeOnWifiMinutes}", xMargin + 10, yPos, textPaint)
        yPos += 20f
        canvas.drawText("- 5G Minutes: ${stats.timeOn5gMinutes}", xMargin + 10, yPos, textPaint)
        yPos += 20f
        canvas.drawText("- LTE Minutes: ${stats.timeOnLteMinutes}", xMargin + 10, yPos, textPaint)
        yPos += 20f
        canvas.drawText("- Legacy Minutes: ${stats.timeOnLegacyMinutes}", xMargin + 10, yPos, textPaint)
        yPos += 20f
        canvas.drawText("- Switch/day: ${"%.2f".format(stats.switchFrequencyPerDay)}", xMargin + 10, yPos, textPaint)
        yPos += 40f

        canvas.drawText("Timeline Log", xMargin, yPos, headerPaint)
        yPos += 20f

        timeline.forEach { item ->
            checkPage()
            val text = "${format(item.event.timestampMs)} | ${item.event.type} | ${item.event.message}"
            canvas.drawText(text, xMargin, yPos, textPaint)
            yPos += 15f
            
            if (item.note != null) {
                checkPage()
                canvas.drawText("  Note: ${item.note}", xMargin + 15, yPos, textPaint)
                yPos += 15f
            }
            yPos += 5f
        }

        document.finishPage(page)

        val file = outputFile("report", "pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        
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
