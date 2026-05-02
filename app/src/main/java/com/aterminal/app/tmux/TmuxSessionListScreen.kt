package com.aterminal.app.tmux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aterminal.app.hosts.ActiveHostSession
import com.aterminal.app.terminal.TerminalSessionSink

@Composable
fun TmuxSessionRoute(
    activeSession: ActiveHostSession?,
    managerFactory: (ActiveHostSession) -> TmuxSessionManager = { session ->
        TmuxRepository(session.controlChannel())
    },
    attacherFactory: (ActiveHostSession) -> TmuxTerminalAttacher = { session ->
        TmuxPtyAttacher(session)
    },
    terminalSessionSink: TerminalSessionSink? = null,
    onAttachedToTerminal: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (activeSession == null) {
        TmuxSessionListScreen(modifier = modifier)
        return
    }

    val manager = remember(activeSession) {
        managerFactory(activeSession)
    }
    val terminalAttacher = remember(activeSession, terminalSessionSink) {
        terminalSessionSink?.let { attacherFactory(activeSession) }
    }
    val routeKey = remember(activeSession) {
        tmuxSessionRouteKey(activeSession)
    }
    val viewModel: TmuxSessionListViewModel = viewModel(
        key = routeKey,
        factory = TmuxSessionListViewModel.Factory(
            manager = manager,
            terminalAttacher = terminalAttacher,
            terminalSessionSink = terminalSessionSink,
            onAttachedToTerminal = onAttachedToTerminal,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(routeKey) {
        viewModel.refresh()
    }

    TmuxSessionListScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onAttach = viewModel::attachSession,
        onRequestKill = viewModel::requestKill,
        onConfirmKill = viewModel::confirmKill,
        onCancelKill = viewModel::cancelKill,
        modifier = modifier,
    )
}

internal fun tmuxSessionRouteKey(activeSession: ActiveHostSession): String {
    return "tmux-session-list-${activeSession.host.id}-${System.identityHashCode(activeSession)}"
}

@Composable
fun TmuxSessionListScreen(
    state: TmuxSessionListUiState = TmuxSessionListUiState(),
    onRefresh: () -> Unit = {},
    onAttach: (String) -> Unit = {},
    onRequestKill: (String) -> Unit = {},
    onConfirmKill: () -> Unit = {},
    onCancelKill: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "tmux Sessions",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Attach or manage active remote agent sessions.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }

            when {
                state.loading -> Text(
                    text = "Loading tmux sessions...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                state.sessions.isEmpty() -> Text(
                    text = "Connect to a host to list active tmux sessions.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.sessions, key = { it.name }) { session ->
                        TmuxSessionRow(
                            session = session,
                            onAttach = { onAttach(session.name) },
                            onRequestKill = { onRequestKill(session.name) },
                        )
                    }
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (state.killConfirmationRequired) {
            AlertDialog(
                onDismissRequest = onCancelKill,
                title = { Text("Kill tmux session?") },
                text = {
                    Text("This will terminate ${state.pendingKillSessionName}.")
                },
                confirmButton = {
                    Button(onClick = onConfirmKill) {
                        Text("Kill")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onCancelKill) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun TmuxSessionRow(
    session: TmuxSession,
    onAttach: () -> Unit,
    onRequestKill: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = session.name,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${session.windowCount} windows · ${if (session.attached) "attached" else "detached"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAttach) {
                    Text("Attach")
                }
                OutlinedButton(onClick = onRequestKill) {
                    Text("Kill")
                }
            }
        }
    }
}
