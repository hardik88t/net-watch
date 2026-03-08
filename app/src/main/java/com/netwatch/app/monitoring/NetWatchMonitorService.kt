package com.netwatch.app.monitoring

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.netwatch.app.NetWatchApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NetWatchMonitorService : LifecycleService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    private val appContainer by lazy {
        (application as NetWatchApplication).appContainer
    }

    private val notificationFactory by lazy {
        NetWatchNotificationFactory(this)
    }

    override fun onCreate() {
        super.onCreate()
        notificationFactory.ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_MANUAL_TEST -> {
                startForeground(MONITOR_NOTIFICATION_ID, notificationFactory.build("Running manual test"))
                serviceScope.launch {
                    val snapshot = appContainer.snapshotProvider.currentSnapshot()
                    appContainer.monitorCoordinator.runManualHeavyTest(snapshot)
                }
            }

            ACTION_START_MONITORING, null -> {
                startForeground(MONITOR_NOTIFICATION_ID, notificationFactory.build())
                startMonitoringIfNeeded()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun startMonitoringIfNeeded() {
        if (monitorJob?.isActive == true) {
            return
        }

        monitorJob = serviceScope.launch {
            while (true) {
                val onboardingCompleted = appContainer.preferencesRepository.onboardingCompleted.first()
                if (!onboardingCompleted) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }

                val constraints = appContainer.preferencesRepository.constraints.first()
                if (!constraints.monitoringEnabled) {
                    delay(5_000)
                    continue
                }

                val snapshot = appContainer.snapshotProvider.currentSnapshot()
                appContainer.monitorCoordinator.processSnapshot(snapshot)
                val probe = appContainer.connectivityChecker.check()
                appContainer.monitorCoordinator.processProbe(snapshot, probe)

                delay(constraints.lightweightCheckIntervalSec * 1000L)
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    companion object {
        fun startIntent(context: android.content.Context): Intent {
            return Intent(context, NetWatchMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
        }

        fun stopIntent(context: android.content.Context): Intent {
            return Intent(context, NetWatchMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
        }

        fun manualTestIntent(context: android.content.Context): Intent {
            return Intent(context, NetWatchMonitorService::class.java).apply {
                action = ACTION_MANUAL_TEST
            }
        }
    }
}
