package com.aterminal.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test
    fun primaryRoutesKeepTheMvpNavigationOrder() {
        val routes = AppRoute.primaryRoutes.map { it.route }

        assertEquals(
            listOf("hosts", "workspaces", "sessions", "terminal", "settings"),
            routes,
        )
    }
}
