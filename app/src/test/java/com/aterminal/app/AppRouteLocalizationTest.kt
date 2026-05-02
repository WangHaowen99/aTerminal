package com.aterminal.app

import com.aterminal.app.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteLocalizationTest {
    @Test
    fun mapsPrimaryRoutesToChineseLabels() {
        assertEquals("主机", AppRoute.Hosts.labelFor(AppLanguage.CHINESE))
        assertEquals("工作区", AppRoute.Workspaces.labelFor(AppLanguage.CHINESE))
        assertEquals("会话", AppRoute.Sessions.labelFor(AppLanguage.CHINESE))
        assertEquals("终端", AppRoute.Terminal.labelFor(AppLanguage.CHINESE))
        assertEquals("设置", AppRoute.Settings.labelFor(AppLanguage.CHINESE))
    }

    @Test
    fun keepsEnglishLabelsByDefault() {
        assertEquals("Hosts", AppRoute.Hosts.labelFor(AppLanguage.ENGLISH))
        assertEquals("Settings", AppRoute.Settings.labelFor(AppLanguage.ENGLISH))
    }
}
