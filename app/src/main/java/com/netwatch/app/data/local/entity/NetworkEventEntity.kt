package com.netwatch.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netwatch.app.core.model.NetworkEventType
import com.netwatch.app.core.model.NetworkTechnology

@Entity(
    tableName = "network_events",
    indices = [Index("timestampMs")]
)
data class NetworkEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val type: NetworkEventType,
    val profileKey: String?,
    val previousTechnology: NetworkTechnology?,
    val currentTechnology: NetworkTechnology?,
    val signalDbm: Int?,
    val message: String,
    val latitude: Double?,
    val longitude: Double?,
    val durationMs: Long?,
    /** When true, this event is excluded from statistics calculations. */
    val isException: Boolean = false,
)
