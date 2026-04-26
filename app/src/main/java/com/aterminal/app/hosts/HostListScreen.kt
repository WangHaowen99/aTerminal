package com.aterminal.app.hosts

import androidx.compose.runtime.Composable
import com.aterminal.app.ui.EmptyStateScreen

@Composable
fun HostListScreen() {
    EmptyStateScreen(
        eyebrow = "ssh control",
        title = "Hosts",
        body = "Add a remote machine to start tmux-backed Codex and Claude Code sessions.",
    )
}
