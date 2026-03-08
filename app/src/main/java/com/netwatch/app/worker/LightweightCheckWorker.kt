package com.netwatch.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.netwatch.app.NetWatchApplication
import kotlinx.coroutines.flow.first

class LightweightCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as NetWatchApplication).appContainer
        val onboardingCompleted = container.preferencesRepository.onboardingCompleted.first()
        if (!onboardingCompleted) {
            return Result.success()
        }
        val constraints = container.preferencesRepository.constraints.first()
        if (!constraints.monitoringEnabled) {
            return Result.success()
        }
        val snapshot = container.snapshotProvider.currentSnapshot()
        container.monitorCoordinator.processSnapshot(snapshot)
        val probe = container.connectivityChecker.check()
        container.monitorCoordinator.processProbe(snapshot, probe)
        return Result.success()
    }
}
