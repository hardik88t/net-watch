package com.netwatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netwatch.app.ui.MainViewModel
import com.netwatch.app.ui.MainViewModelFactory
import com.netwatch.app.ui.NetWatchApp
import com.netwatch.app.ui.theme.NetWatchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as NetWatchApplication).appContainer

        setContent {
            NetWatchTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(
                        application = application,
                        monitoringRepository = appContainer.monitoringRepository,
                        preferencesRepository = appContainer.preferencesRepository,
                        reportExporter = appContainer.reportExporter,
                    )
                )

                NetWatchApp(viewModel = viewModel)
            }
        }
    }
}
