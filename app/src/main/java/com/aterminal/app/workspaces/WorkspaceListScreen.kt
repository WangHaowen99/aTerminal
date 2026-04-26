package com.aterminal.app.workspaces

import androidx.compose.runtime.Composable
import com.aterminal.app.ui.EmptyStateScreen

@Composable
fun WorkspaceListScreen() {
    EmptyStateScreen(
        eyebrow = "agent launchpad",
        title = "Workspaces",
        body = "Save project directories, then resume Codex or Claude Code in a few taps.",
    )
}
