package com.netwatch.app.monitoring

import com.netwatch.app.core.model.ConnectionSnapshot

interface NetworkSnapshotProvider {
    suspend fun currentSnapshot(): ConnectionSnapshot
}
