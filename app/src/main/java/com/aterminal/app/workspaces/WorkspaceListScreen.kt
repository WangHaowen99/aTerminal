package com.aterminal.app.workspaces

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aterminal.app.agents.WorkspaceAgentAction
import com.aterminal.app.agents.WorkspaceAgentActionAvailability
import com.aterminal.app.data.WorkspaceEntity
import com.aterminal.app.hosts.RemoteCapabilities
import com.aterminal.app.ui.EmptyStateScreen

@Composable
fun WorkspaceListScreen(
    workspaces: List<WorkspaceEntity> = emptyList(),
    capabilities: RemoteCapabilities? = null,
    onActionSelected: (WorkspaceEntity, WorkspaceAgentAction) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    if (workspaces.isEmpty()) {
        EmptyStateScreen(
            eyebrow = "agent launchpad",
            title = "Workspaces",
            body = "Save project directories, then resume Codex or Claude Code in a few taps.",
            modifier = modifier,
        )
        return
    }

    var advancedWorkspace by remember { mutableStateOf<WorkspaceEntity?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Workspaces",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            items(
                items = workspaces,
                key = { it.id },
            ) { workspace ->
                WorkspaceCard(
                    workspace = workspace,
                    capabilities = capabilities,
                    onActionSelected = onActionSelected,
                    onAdvancedClick = { advancedWorkspace = workspace },
                )
            }
        }
    }

    advancedWorkspace?.let { workspace ->
        ResumeByIdDialog(
            workspace = workspace,
            capabilities = capabilities,
            onDismiss = { advancedWorkspace = null },
            onActionSelected = { action ->
                onActionSelected(workspace, action)
                advancedWorkspace = null
            },
        )
    }
}

@Composable
private fun WorkspaceCard(
    workspace: WorkspaceEntity,
    capabilities: RemoteCapabilities?,
    onActionSelected: (WorkspaceEntity, WorkspaceAgentAction) -> Unit,
    onAdvancedClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = workspace.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = workspace.remoteCwd,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Preferred: ${workspace.preferredAgent.remoteName}") },
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WorkspaceAgentAction.primaryActions.forEach { action ->
                    val availability = action.availability(capabilities)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = availability.enabled,
                        onClick = { onActionSelected(workspace, action) },
                    ) {
                        Text(action.label)
                    }
                    availability.disabledReason?.let { reason ->
                        Text(
                            text = reason,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = WorkspaceAgentAction.ResumeCodexLatest
                        .availability(capabilities)
                        .enabled ||
                        WorkspaceAgentAction.ContinueClaude
                            .availability(capabilities)
                            .enabled,
                    onClick = onAdvancedClick,
                ) {
                    Text("Advanced")
                }
            }
        }
    }
}

@Composable
private fun ResumeByIdDialog(
    workspace: WorkspaceEntity,
    capabilities: RemoteCapabilities?,
    onDismiss: () -> Unit,
    onActionSelected: (WorkspaceAgentAction) -> Unit,
) {
    var sessionId by remember(workspace.id) { mutableStateOf("") }
    val canSubmit = sessionId.isNotBlank()
    val codexAction = remember(sessionId) {
        sessionId.takeIf { it.isNotBlank() }?.let(WorkspaceAgentAction::resumeCodexById)
    }
    val claudeAction = remember(sessionId) {
        sessionId.takeIf { it.isNotBlank() }?.let(WorkspaceAgentAction::resumeClaudeById)
    }
    val codexAvailability = codexAction?.availability(capabilities)
        ?: WorkspaceAgentActionAvailability.Disabled("Agent session id is required.")
    val claudeAvailability = claudeAction?.availability(capabilities)
        ?: WorkspaceAgentActionAvailability.Disabled("Agent session id is required.")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resume ${workspace.name}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Paste a Codex or Claude Code session id to create a dedicated tmux session.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = sessionId,
                    onValueChange = { sessionId = it },
                    label = { Text("Agent session id") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                listOfNotNull(
                    codexAvailability.disabledReason,
                    claudeAvailability.disabledReason,
                ).distinct().forEach { reason ->
                    Text(
                        text = reason,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = canSubmit && codexAvailability.enabled,
                    onClick = {
                        codexAction?.let(onActionSelected)
                    },
                ) {
                    Text("Resume Codex by ID")
                }
                Button(
                    enabled = canSubmit && claudeAvailability.enabled,
                    onClick = {
                        claudeAction?.let(onActionSelected)
                    },
                ) {
                    Text("Resume Claude by ID")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
