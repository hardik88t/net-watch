package com.netwatch.app.core.model

data class TimelineItem(
    val event: NetworkEvent,
    val note: String? = null,
)
