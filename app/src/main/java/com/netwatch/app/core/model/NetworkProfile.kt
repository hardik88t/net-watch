package com.netwatch.app.core.model

enum class ProfileType {
    SIM,
    WIFI,
    UNKNOWN
}

data class NetworkProfile(
    val key: String,
    val displayName: String,
    val type: ProfileType,
    val carrierName: String? = null,
    val simSlotIndex: Int? = null,
    val subscriptionId: Int? = null,
    val ssid: String? = null,
    val bssid: String? = null,
)
