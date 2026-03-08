package com.netwatch.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_profiles")
data class NetworkProfileEntity(
    @PrimaryKey val key: String,
    val displayName: String,
    val type: String,
    val carrierName: String?,
    val simSlotIndex: Int?,
    val subscriptionId: Int?,
    val ssid: String?,
    val bssid: String?,
    val lastSeenAtMs: Long,
)
