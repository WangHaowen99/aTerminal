package com.aterminal.app.terminal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickKeyBar(
    onKey: (TerminalQuickKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TerminalQuickKey.entries.forEach { key ->
            AssistChip(
                onClick = { onKey(key) },
                label = { Text(key.label) },
            )
        }
    }
}

enum class TerminalQuickKey(
    val label: String,
    val sequence: String,
) {
    Escape("Esc", "\u001B"),
    Tab("Tab", "\t"),
    ArrowUp("Up", "\u001B[A"),
    ArrowDown("Down", "\u001B[B"),
    ArrowLeft("Left", "\u001B[D"),
    ArrowRight("Right", "\u001B[C"),
    ControlC("Ctrl+C", "\u0003"),
    ControlD("Ctrl+D", "\u0004"),
    TmuxPrefix("tmux", "\u0002"),
}
