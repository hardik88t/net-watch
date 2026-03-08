package com.netwatch.app.monitoring

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.SpeedTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class HybridSpeedTestExecutor(
    private val downloadUrl: String = "https://speed.cloudflare.com/__down?bytes=2000000",
    private val uploadUrl: String = "https://httpbin.org/post",
) : SpeedTestExecutor {

    override suspend fun run(triggerReason: String, snapshot: ConnectionSnapshot): SpeedTestResult {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            runCatching {
                val latencyMs = measureLatency()
                val (downloadMbps, downloadedBytes) = measureDownloadMbps()
                val (uploadMbps, uploadedBytes) = measureUploadMbps()
                SpeedTestResult(
                    timestampMs = now,
                    triggerReason = triggerReason,
                    profileKey = snapshot.profile.key,
                    downloadMbps = downloadMbps,
                    uploadMbps = uploadMbps,
                    latencyMs = latencyMs,
                    isVpnActive = snapshot.isVpnActive,
                    isProxyActive = snapshot.isProxyActive,
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude,
                    bytesConsumed = downloadedBytes + uploadedBytes,
                    estimated = false,
                )
            }.getOrElse {
                val estimatedDown = estimateDownFromSignal(snapshot.signalDbm)
                SpeedTestResult(
                    timestampMs = now,
                    triggerReason = "$triggerReason (estimated)",
                    profileKey = snapshot.profile.key,
                    downloadMbps = estimatedDown,
                    uploadMbps = max(1.0, estimatedDown / 3.0),
                    latencyMs = estimateLatencyFromSignal(snapshot.signalDbm),
                    isVpnActive = snapshot.isVpnActive,
                    isProxyActive = snapshot.isProxyActive,
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude,
                    bytesConsumed = 0,
                    estimated = true,
                )
            }
        }
    }

    private fun measureLatency(): Double {
        val attempts = 3
        val values = mutableListOf<Long>()
        repeat(attempts) {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 443), 1500)
            }
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            values += elapsedMs
        }
        return values.average()
    }

    private fun measureDownloadMbps(): Pair<Double, Long> {
        val urlConnection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4_000
            readTimeout = 4_000
            useCaches = false
        }

        val start = System.nanoTime()
        val totalBytes = urlConnection.inputStream.use { raw ->
            BufferedInputStream(raw).use { input ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead = input.read(buffer)
                var byteCount = 0L
                while (bytesRead >= 0) {
                    byteCount += bytesRead
                    bytesRead = input.read(buffer)
                }
                byteCount
            }
        }
        urlConnection.disconnect()

        val elapsedSec = (System.nanoTime() - start) / 1_000_000_000.0
        val mbps = (totalBytes * 8.0 / 1_000_000.0) / elapsedSec
        return mbps to totalBytes
    }

    private fun measureUploadMbps(): Pair<Double, Long> {
        val bytes = ByteArray(256 * 1024)
        Random.nextBytes(bytes)

        val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 4_000
            readTimeout = 4_000
            setRequestProperty("Content-Type", "application/octet-stream")
            setRequestProperty("Content-Length", bytes.size.toString())
        }

        val start = System.nanoTime()
        connection.outputStream.use { output: OutputStream ->
            output.write(bytes)
            output.flush()
        }
        connection.inputStream.close()
        connection.disconnect()

        val elapsedSec = (System.nanoTime() - start) / 1_000_000_000.0
        val mbps = (bytes.size * 8.0 / 1_000_000.0) / elapsedSec
        return mbps to bytes.size.toLong()
    }

    private fun estimateDownFromSignal(signalDbm: Int?): Double {
        val signal = signalDbm ?: -100
        return when {
            signal >= -75 -> 180.0
            signal >= -85 -> 110.0
            signal >= -95 -> 45.0
            signal >= -105 -> 12.0
            else -> 2.0
        }
    }

    private fun estimateLatencyFromSignal(signalDbm: Int?): Double {
        val signal = signalDbm ?: -100
        val base = when {
            signal >= -75 -> 20
            signal >= -85 -> 35
            signal >= -95 -> 55
            signal >= -105 -> 90
            else -> 140
        }
        return (base + Random.nextInt(1, 12)).toDouble().roundToInt().toDouble()
    }
}
