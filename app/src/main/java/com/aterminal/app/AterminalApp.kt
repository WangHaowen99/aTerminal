package com.aterminal.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aterminal.app.hosts.HostListScreen
import com.aterminal.app.settings.SettingsScreen
import com.aterminal.app.terminal.TerminalScreen
import com.aterminal.app.theme.ATerminalTheme
import com.aterminal.app.workspaces.WorkspaceListScreen

@Composable
fun AterminalApp() {
    ATerminalTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: AppRoute.Hosts.route

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    AppRoute.primaryRoutes.forEach { route ->
                        NavigationBarItem(
                            selected = currentRoute == route.route,
                            onClick = {
                                navController.navigate(route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text(route.label) },
                            icon = { Text(route.iconText) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.Hosts.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(AppRoute.Hosts.route) { HostListScreen() }
                composable(AppRoute.Workspaces.route) { WorkspaceListScreen() }
                composable(AppRoute.Terminal.route) { TerminalScreen() }
                composable(AppRoute.Settings.route) { SettingsScreen() }
            }
        }
    }
}
