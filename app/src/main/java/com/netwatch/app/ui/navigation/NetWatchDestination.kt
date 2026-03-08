package com.netwatch.app.ui.navigation

enum class NetWatchDestination(
    val route: String,
    val label: String,
) {
    DASHBOARD("dashboard", "Dashboard"),
    TIMELINE("timeline", "Timeline"),
    STATS("stats", "Stats"),
    SETTINGS("settings", "Settings"),
}
