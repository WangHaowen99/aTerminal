package com.aterminal.app.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TerminalScreen(
    viewModel: TerminalSessionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TerminalContent(
        state = state,
        onInput = viewModel::sendText,
        onPaste = viewModel::requestPaste,
        onConfirmPaste = viewModel::confirmPaste,
        onCancelPaste = viewModel::cancelPaste,
        onQuickKey = viewModel::sendQuickKey,
        onViewportResize = viewModel::resize,
    )
}

@Composable
fun TerminalContent(
    state: TerminalUiState,
    onInput: (String) -> Unit,
    onPaste: (String) -> Unit,
    onConfirmPaste: () -> Unit,
    onCancelPaste: () -> Unit,
    onQuickKey: (TerminalQuickKey) -> Unit,
    onViewportResize: (columns: Int, rows: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var draftInput by remember { mutableStateOf("") }
    var lastViewport by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        val columns = (size.width / APPROXIMATE_CELL_WIDTH_PX)
                            .coerceAtLeast(MIN_TERMINAL_COLUMNS)
                        val rows = (size.height / APPROXIMATE_CELL_HEIGHT_PX)
                            .coerceAtLeast(MIN_TERMINAL_ROWS)
                        val viewport = columns to rows
                        if (viewport != lastViewport) {
                            lastViewport = viewport
                            onViewportResize(columns, rows)
                        }
                    }
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    text = state.screenText.ifBlank { "No PTY attached. Launch or attach a tmux session to stream terminal output here." },
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            QuickKeyBar(onKey = onQuickKey)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = draftInput,
                    onValueChange = { draftInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    ),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        onInput("$draftInput\n")
                        draftInput = ""
                    },
                ) {
                    Text("Send")
                }
                OutlinedButton(
                    onClick = {
                        clipboard.getText()?.text?.let(onPaste)
                    },
                ) {
                    Text("Paste")
                }
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(state.screenText))
                    },
                ) {
                    Text("Copy")
                }
            }
        }

        if (state.pasteConfirmationRequired) {
            AlertDialog(
                onDismissRequest = onCancelPaste,
                title = { Text("Confirm multiline paste") },
                text = { Text("This paste spans multiple lines and will be sent directly to the remote shell.") },
                confirmButton = {
                    Button(onClick = onConfirmPaste) {
                        Text("Paste")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onCancelPaste) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

private const val APPROXIMATE_CELL_HEIGHT_PX = 28
private const val APPROXIMATE_CELL_WIDTH_PX = 14
private const val MIN_TERMINAL_COLUMNS = 20
private const val MIN_TERMINAL_ROWS = 5
