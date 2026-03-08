package com.netwatch.app.ui.screen

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.netwatch.app.R
import com.netwatch.app.ui.theme.NetWatchAccent
import com.netwatch.app.ui.theme.NetWatchBackground
import com.netwatch.app.ui.theme.NetWatchPrimaryText
import com.netwatch.app.ui.theme.NetWatchSecondaryText
import com.netwatch.app.ui.theme.NetWatchSurface

@Composable
fun OnboardingScreen(
    onGrantAccess: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissions by remember { mutableStateOf(readPermissionState(context)) }
    var showSkipWarning by remember { mutableStateOf(false) }
    var showUsageAccessHint by remember { mutableStateOf(false) }

    fun refreshState() {
        permissions = readPermissionState(context)
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        refreshState()
        if (permissions.allRequiredGranted) {
            onGrantAccess()
        }
    }

    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshState()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            permissions.fineLocationGranted &&
            !permissions.backgroundLocationGranted
        ) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else if (permissions.allRequiredGranted) {
            onGrantAccess()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetWatchBackground)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.netwatch_logo),
                contentDescription = "NetWatch logo",
                modifier = Modifier.size(40.dp),
            )
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = NetWatchPrimaryText,
                modifier = Modifier.padding(start = 8.dp),
            )
            Text(
                text = "Permission Setup",
                modifier = Modifier.padding(start = 12.dp),
                color = NetWatchPrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(NetWatchSurface, NetWatchAccent.copy(alpha = 0.25f), NetWatchSurface)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(43.dp))
                    .background(NetWatchAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.NetworkCell,
                    contentDescription = null,
                    tint = NetWatchAccent,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Text(
            text = "Help us map the network",
            color = NetWatchPrimaryText,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Grant access for accurate transitions, outages, and diagnostics. Usage Access is needed to correlate network behavior with system-level usage stats.",
            color = NetWatchSecondaryText,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )

        PermissionItem(
            title = "Fine Location",
            description = "Required for geotagging each transition and outage.",
            granted = permissions.fineLocationGranted,
            icon = Icons.Rounded.LocationOn,
        )

        PermissionItem(
            title = "Background Location",
            description = "Required to keep location tagging while app is not in foreground.",
            granted = permissions.backgroundLocationGranted,
            icon = Icons.Rounded.LocationOn,
        )

        PermissionItem(
            title = "Phone State",
            description = "Required for carrier, SIM route, and LTE/5G signal telemetry.",
            granted = permissions.phoneStateGranted,
            icon = Icons.Rounded.PhoneAndroid,
        )

        PermissionItem(
            title = "Notifications",
            description = "Required to display persistent foreground monitoring status.",
            granted = permissions.notificationsGranted,
            icon = Icons.Rounded.Notifications,
        )

        PermissionItem(
            title = "Usage Access",
            description = "Enable Usage Access from system settings to improve data-integrity checks.",
            granted = permissions.usageAccessGranted,
            icon = Icons.Rounded.NetworkCell,
            trailingAction = {
                if (!permissions.usageAccessGranted) {
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        }
                    ) {
                        Text("Open")
                    }
                }
            },
        )

        Button(
            onClick = {
                showUsageAccessHint = false
                if (permissions.allRequiredGranted) {
                    onGrantAccess()
                    return@Button
                }

                val runtimePermissions = buildList {
                    if (!permissions.fineLocationGranted) {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    if (!permissions.phoneStateGranted) {
                        add(Manifest.permission.READ_PHONE_STATE)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.notificationsGranted) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                when {
                    runtimePermissions.isNotEmpty() -> {
                        runtimePermissionsLauncher.launch(runtimePermissions.toTypedArray())
                    }

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !permissions.backgroundLocationGranted -> {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }

                    !permissions.usageAccessGranted -> {
                        showUsageAccessHint = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NetWatchAccent,
                contentColor = NetWatchBackground,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = if (permissions.allRequiredGranted) "Start Monitoring" else "Grant Required Access",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }

        if (showUsageAccessHint && !permissions.usageAccessGranted) {
            Text(
                text = "Usage Access is still disabled. Tap Open on the Usage Access row, grant NetWatch, then return here.",
                color = NetWatchSecondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }

        TextButton(
            onClick = { showSkipWarning = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Skip with limited diagnostics")
        }
    }

    if (showSkipWarning) {
        AlertDialog(
            onDismissRequest = { showSkipWarning = false },
            title = { Text("Skip required access?") },
            text = {
                Text(
                    text = buildString {
                        append("Monitoring will run with partial or missing data. Missing access: ")
                        append(permissions.missingRequirements().joinToString(", "))
                        append(". You can enable permissions later from Android settings.")
                    },
                    color = NetWatchSecondaryText,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSkipWarning = false
                        onSkip()
                    }
                ) {
                    Text("Skip Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipWarning = false }) {
                    Text("Continue Setup")
                }
            },
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingAction: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NetWatchSurface.copy(alpha = 0.85f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NetWatchAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = NetWatchAccent)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(text = title, color = NetWatchPrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(text = description, color = NetWatchSecondaryText, fontSize = 14.sp)
        }

        if (trailingAction != null) {
            trailingAction()
        } else {
            Switch(checked = granted, onCheckedChange = null, enabled = false)
        }
    }
}

private data class OnboardingPermissionState(
    val fineLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val phoneStateGranted: Boolean,
    val notificationsGranted: Boolean,
    val usageAccessGranted: Boolean,
) {
    val allRequiredGranted: Boolean
        get() = fineLocationGranted &&
            backgroundLocationGranted &&
            phoneStateGranted &&
            notificationsGranted &&
            usageAccessGranted

    fun missingRequirements(): List<String> = buildList {
        if (!fineLocationGranted) add("Fine Location")
        if (!backgroundLocationGranted) add("Background Location")
        if (!phoneStateGranted) add("Phone State")
        if (!notificationsGranted) add("Notifications")
        if (!usageAccessGranted) add("Usage Access")
    }
}

private fun readPermissionState(context: Context): OnboardingPermissionState {
    val fineLocationGranted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        true
    }
    val phoneStateGranted = hasPermission(context, Manifest.permission.READ_PHONE_STATE)
    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        true
    }

    return OnboardingPermissionState(
        fineLocationGranted = fineLocationGranted,
        backgroundLocationGranted = backgroundLocationGranted,
        phoneStateGranted = phoneStateGranted,
        notificationsGranted = notificationsGranted,
        usageAccessGranted = hasUsageAccess(context),
    )
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun hasUsageAccess(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    } else {
        @Suppress("DEPRECATION")
        appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    }

    if (mode == AppOpsManager.MODE_ALLOWED) {
        return true
    }

    if (mode == AppOpsManager.MODE_DEFAULT) {
        return context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
    }

    return false
}
