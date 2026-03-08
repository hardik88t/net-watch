package com.netwatch.app.monitoring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HttpLightweightConnectivityChecker(
    private val endpoint: String = "https://connectivitycheck.gstatic.com/generate_204",
    private val connectTimeoutMs: Int = 2500,
    private val readTimeoutMs: Int = 2500,
) : LightweightConnectivityChecker {

    override suspend fun check(): ConnectivityProbeResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                useCaches = false
                instanceFollowRedirects = false
            }
            connection.connect()
            val status = connection.responseCode
            connection.inputStream.close()
            connection.disconnect()
            ConnectivityProbeResult(
                success = status in 200..299,
                host = endpoint,
                latencyMs = System.currentTimeMillis() - started,
                checkedAtMs = System.currentTimeMillis(),
                error = if (status in 200..299) null else "HTTP $status",
            )
        }.getOrElse { error ->
            ConnectivityProbeResult(
                success = false,
                host = endpoint,
                latencyMs = System.currentTimeMillis() - started,
                checkedAtMs = System.currentTimeMillis(),
                error = error.message,
            )
        }
    }
}
