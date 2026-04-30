package com.aterminal.app.hosts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aterminal.app.AterminalApplication
import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity

@Composable
fun HostRoute() {
    val application = LocalContext.current.applicationContext as AterminalApplication
    val viewModel: HostListViewModel = viewModel(
        factory = HostListViewModel.Factory(
            hostStore = application.hostRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    HostListScreen(
        state = state,
        onAddHostClick = viewModel::showAddHostDialog,
        onDismissAddHost = viewModel::dismissAddHostDialog,
        onFormChange = viewModel::updateForm,
        onSaveHost = viewModel::saveHost,
        onDeleteHost = viewModel::deleteHost,
    )
}

@Composable
fun HostListScreen(
    state: HostListUiState,
    onAddHostClick: () -> Unit,
    onDismissAddHost: () -> Unit,
    onFormChange: (HostFormState) -> Unit,
    onSaveHost: () -> Unit,
    onDeleteHost: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HostListHeader(onAddHostClick = onAddHostClick)
            }
            state.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (state.hosts.isEmpty()) {
                item {
                    Text(
                        text = "Add a remote machine to start tmux-backed Codex and Claude Code sessions.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                items(
                    items = state.hosts,
                    key = { it.id },
                ) { host ->
                    HostCard(
                        host = host,
                        onDeleteHost = onDeleteHost,
                    )
                }
            }
        }
    }

    if (state.showAddHostDialog) {
        AddHostDialog(
            form = state.form,
            errorMessage = state.errorMessage,
            onFormChange = onFormChange,
            onDismiss = onDismissAddHost,
            onSave = onSaveHost,
        )
    }
}

@Composable
private fun HostListHeader(
    onAddHostClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "SSH CONTROL",
            color = MaterialTheme.colorScheme.secondary,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Hosts",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
            )
            Button(onClick = onAddHostClick) {
                Text("Add host")
            }
        }
    }
}

@Composable
private fun HostCard(
    host: HostEntity,
    onDeleteHost: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = host.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${host.username}@${host.hostname}:${host.port}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            AssistChip(
                onClick = {},
                label = { Text(host.authType.displayLabel) },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = {}) {
                    Text("Connect")
                }
                OutlinedButton(onClick = { onDeleteHost(host.id) }) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun AddHostDialog(
    form: HostFormState,
    errorMessage: String?,
    onFormChange: (HostFormState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add SSH host") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = form.displayName,
                    onValueChange = { onFormChange(form.copy(displayName = it)) },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.hostname,
                    onValueChange = { onFormChange(form.copy(hostname = it)) },
                    label = { Text("Hostname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.portText,
                    onValueChange = { onFormChange(form.copy(portText = it)) },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.username,
                    onValueChange = { onFormChange(form.copy(username = it)) },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AuthType.entries.forEach { authType ->
                        val selected = authType == form.authType
                        if (selected) {
                            Button(onClick = { onFormChange(form.copy(authType = authType)) }) {
                                Text(authType.displayLabel)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onFormChange(form.copy(authType = authType)) },
                            ) {
                                Text(authType.displayLabel)
                            }
                        }
                    }
                }
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save host")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private val AuthType.displayLabel: String
    get() = when (this) {
        AuthType.PASSWORD -> "Password"
        AuthType.PRIVATE_KEY -> "Private key"
    }
