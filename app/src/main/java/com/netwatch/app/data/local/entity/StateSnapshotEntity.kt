package com.netwatch.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.netwatch.app.core.model.NetworkTechnology

@Entity(tableName = "state_snapshots")
data class StateSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val profileKey: String,
    val profileLabel: String,
    val profileType: String,
    val technology: NetworkTechnology,
    val signalDbm: Int?,
    val hasInternet: Boolean,
    val isVpnActive: Boolean,
    val isProxyActive: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
)
