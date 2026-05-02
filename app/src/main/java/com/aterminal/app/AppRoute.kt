package com.aterminal.app

import com.aterminal.app.settings.AppLanguage

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

fun AppRoute.labelFor(language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) {
        return label
    }

    return when (this) {
        AppRoute.Hosts -> "主机"
        AppRoute.Workspaces -> "工作区"
        AppRoute.Sessions -> "会话"
        AppRoute.Terminal -> "终端"
        AppRoute.Settings -> "设置"
    }
}
