package com.aterminal.app.settings

import androidx.compose.runtime.Composable
import com.aterminal.app.ui.EmptyStateScreen

@Composable
fun SettingsScreen() {
    EmptyStateScreen(
        eyebrow = "local policy",
        title = "Settings",
        body = "Configure default agent flags, SSH behavior, and reading-mode preferences.",
    )
}
