package com.aterminal.app.terminal

import androidx.compose.runtime.Composable
import com.aterminal.app.ui.EmptyStateScreen

@Composable
fun TerminalScreen() {
    EmptyStateScreen(
        eyebrow = "pty viewport",
        title = "Terminal",
        body = "Interactive SSH output will appear here once a host or tmux session is attached.",
    )
}
