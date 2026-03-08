package com.netwatch.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "annotations",
    indices = [Index("eventId"), Index("timestampMs")]
)
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long?,
    val timestampMs: Long,
    val text: String,
)
