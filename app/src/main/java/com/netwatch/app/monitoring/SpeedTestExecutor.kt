package com.netwatch.app.monitoring

import com.netwatch.app.core.model.ConnectionSnapshot
import com.netwatch.app.core.model.SpeedTestResult

interface SpeedTestExecutor {
    suspend fun run(triggerReason: String, snapshot: ConnectionSnapshot): SpeedTestResult
}
