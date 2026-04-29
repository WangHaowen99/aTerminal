package com.aterminal.app

enum class AppRoute(
    val route: String,
    val label: String,
    val iconText: String,
) {
    Hosts("hosts", "Hosts", "SSH"),
    Workspaces("workspaces", "Workspaces", "WS"),
    Sessions("sessions", "Sessions", "MUX"),
    Terminal("terminal", "Terminal", ">_"),
    Settings("settings", "Settings", "CFG");

    companion object {
        val primaryRoutes: List<AppRoute> = entries.toList()
    }
}
