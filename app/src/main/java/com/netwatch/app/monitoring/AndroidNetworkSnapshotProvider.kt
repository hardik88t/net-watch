package com.netwatch.app.monitoring

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.NetworkProfile
import com.netwatch.app.core.model.NetworkTechnology
import com.netwatch.app.core.model.ProfileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidNetworkSnapshotProvider(
    context: Context,
) : NetworkSnapshotProvider {

    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override suspend fun currentSnapshot(): ConnectionSnapshot = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isVpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val hasValidatedInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        val profile = when {
            isWifi -> wifiProfile()
            isCellular -> cellularProfile()
            else -> NetworkProfile(
                key = "unknown",
                displayName = "No active profile",
                type = ProfileType.UNKNOWN,
            )
        }

        val technology = when {
            isWifi -> NetworkTechnology.WIFI
            isCellular -> mapTelephonyType()
            emergencyOnly() -> NetworkTechnology.EMERGENCY_ONLY
            else -> NetworkTechnology.NO_SERVICE
        }

        val signalDbm = when {
            isWifi -> wifiSignalDbm()
            isCellular -> cellularSignalDbm()
            else -> null
        }

        val location = lastKnownLocation()

        ConnectionSnapshot(
            timestampMs = now,
            profile = profile,
            technology = technology,
            signalDbm = signalDbm,
            hasInternet = hasValidatedInternet,
            isVpnActive = isVpn,
            isProxyActive = isProxyConfigured(),
            latitude = location?.latitude,
            longitude = location?.longitude,
            totalRxBytes = TrafficStats.getTotalRxBytes(),
            totalTxBytes = TrafficStats.getTotalTxBytes(),
        )
    }

    private fun wifiProfile(): NetworkProfile {
        val info = wifiManager.connectionInfo
        val ssid = info?.ssid?.trim('"')
        val bssid = info?.bssid
        val key = bssid ?: "wifi:${ssid ?: "unknown"}"
        return NetworkProfile(
            key = key,
            displayName = ssid ?: "Wi-Fi",
            type = ProfileType.WIFI,
            ssid = ssid,
            bssid = bssid,
        )
    }

    private fun cellularProfile(): NetworkProfile {
        val subscriptionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            SubscriptionManager.getActiveDataSubscriptionId().takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
        } else {
            null
        }

        val carrier = runCatching { telephonyManager.networkOperatorName }.getOrNull().orEmpty()
        val key = subscriptionId?.let { "sim:$it" } ?: "sim:${carrier.ifBlank { "unknown" }}"

        return NetworkProfile(
            key = key,
            displayName = if (carrier.isBlank()) "Cellular" else carrier,
            type = ProfileType.SIM,
            carrierName = carrier.ifBlank { null },
            subscriptionId = subscriptionId,
        )
    }

    private fun emergencyOnly(): Boolean {
        return runCatching {
            telephonyManager.serviceState?.state == ServiceState.STATE_EMERGENCY_ONLY
        }.getOrDefault(false)
    }

    private fun mapTelephonyType(): NetworkTechnology {
        val type = runCatching { telephonyManager.dataNetworkType }.getOrDefault(TelephonyManager.NETWORK_TYPE_UNKNOWN)
        return when (type) {
            TelephonyManager.NETWORK_TYPE_NR -> NetworkTechnology.NETWORK_5G
            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_IWLAN,
            -> NetworkTechnology.LTE

            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN,
            TelephonyManager.NETWORK_TYPE_GSM,
            -> NetworkTechnology.NETWORK_2G

            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA,
            -> NetworkTechnology.NETWORK_3G

            else -> if (emergencyOnly()) NetworkTechnology.EMERGENCY_ONLY else NetworkTechnology.NO_SERVICE
        }
    }

    private fun cellularSignalDbm(): Int? {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            return null
        }
        return runCatching {
            telephonyManager.signalStrength
                ?.cellSignalStrengths
                ?.firstOrNull()
                ?.dbm
        }.getOrNull()
    }

    private fun wifiSignalDbm(): Int? {
        return runCatching { wifiManager.connectionInfo?.rssi }.getOrNull()
    }

    private fun lastKnownLocation(): Location? {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return null
        }

        val gps = runCatching { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        val network = runCatching { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()

        return listOfNotNull(gps, network).maxByOrNull { it.time }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isProxyConfigured(): Boolean {
        val host = System.getProperty("http.proxyHost")
        return !host.isNullOrBlank()
    }
}
