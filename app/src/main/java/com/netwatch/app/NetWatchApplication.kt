package com.netwatch.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.netwatch.app.worker.LightweightCheckWorker
import org.osmdroid.config.Configuration
import java.util.concurrent.TimeUnit

class NetWatchApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        appContainer = AppContainer(this)
        scheduleLightweightWorker()
    }

    private fun scheduleLightweightWorker() {
        val request = PeriodicWorkRequestBuilder<LightweightCheckWorker>(15, TimeUnit.MINUTES)
            .addTag("netwatch-lightweight-check")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "netwatch-lightweight-check",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
