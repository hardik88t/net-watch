package com.netwatch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NetWatchBackground = Color(0xFF0F231E)
val NetWatchSurface = Color(0xFF103129)
val NetWatchSurfaceVariant = Color(0x33116A56)
val NetWatchAccent = Color(0xFF66FFDB)
val NetWatchPrimaryText = Color(0xFFF1F5F9)
val NetWatchSecondaryText = Color(0xFF94A3B8)
val NetWatchDanger = Color(0xFFF43F5E)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = NetWatchAccent,
    onPrimary = NetWatchBackground,
    background = NetWatchBackground,
    onBackground = NetWatchPrimaryText,
    surface = NetWatchSurface,
    onSurface = NetWatchPrimaryText,
    surfaceVariant = NetWatchSurfaceVariant,
    onSurfaceVariant = NetWatchSecondaryText,
    error = NetWatchDanger,
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0B9E84),
    onPrimary = Color.White,
    background = Color(0xFFF6FAF9),
    onBackground = Color(0xFF0C1721),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0C1721),
    surfaceVariant = Color(0xFFE2F0EC),
    onSurfaceVariant = Color(0xFF4C5F6D),
    error = NetWatchDanger,
)

@Composable
fun NetWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
