package com.netwatch.app.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.netwatch.app.NetWatchApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as NetWatchApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val onboardingCompleted = app.appContainer.preferencesRepository.onboardingCompleted.first()
                if (!onboardingCompleted) {
                    return@launch
                }
                val constraints = app.appContainer.preferencesRepository.constraints.first()
                if (constraints.autoResumeOnBoot && constraints.monitoringEnabled) {
                    runCatching {
                        ContextCompat.startForegroundService(context, NetWatchMonitorService.startIntent(context))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
