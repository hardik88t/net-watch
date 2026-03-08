package com.netwatch.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "speed_test_results",
    indices = [Index("timestampMs")]
)
data class SpeedTestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val triggerReason: String,
    val profileKey: String?,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Double,
    val isVpnActive: Boolean,
    val isProxyActive: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val bytesConsumed: Long,
    val estimated: Boolean,
)
