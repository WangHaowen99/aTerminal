package com.aterminal.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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

@Composable
fun SettingsRoute() {
    val application = LocalContext.current.applicationContext as AterminalApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            settingsRepository = application.agentSettingsRepository,
            metadataTransferRepository = application.metadataTransferRepository,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onCodexDefaultFlagsChange = viewModel::updateCodexDefaultFlags,
        onClaudeDefaultFlagsChange = viewModel::updateClaudeDefaultFlags,
        onExportMetadata = viewModel::exportMetadata,
        onImportPayloadChange = viewModel::updateImportPayload,
        onImportMetadata = viewModel::importMetadata,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onCodexDefaultFlagsChange: (String) -> Unit,
    onClaudeDefaultFlagsChange: (String) -> Unit,
    onExportMetadata: () -> Unit,
    onImportPayloadChange: (String) -> Unit,
    onImportMetadata: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsHeader()
            AgentDefaultsCard(
                codexDefaultFlags = state.codexDefaultFlags,
                claudeDefaultFlags = state.claudeDefaultFlags,
                onCodexDefaultFlagsChange = onCodexDefaultFlagsChange,
                onClaudeDefaultFlagsChange = onClaudeDefaultFlagsChange,
            )
            MetadataTransferCard(
                state = state,
                onExportMetadata = onExportMetadata,
                onImportPayloadChange = onImportPayloadChange,
                onImportMetadata = onImportMetadata,
            )
            PrivacyCard()
        }
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "LOCAL POLICY",
            color = MaterialTheme.colorScheme.secondary,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "Settings",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Configure default agent flags, SSH behavior, and reading-mode preferences.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun AgentDefaultsCard(
    codexDefaultFlags: String,
    claudeDefaultFlags: String,
    onCodexDefaultFlagsChange: (String) -> Unit,
    onClaudeDefaultFlagsChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Agent defaults",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = codexDefaultFlags,
                onValueChange = onCodexDefaultFlagsChange,
                label = { Text("Codex default flags") },
                placeholder = { Text("--model gpt-5.4") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = claudeDefaultFlags,
                onValueChange = onClaudeDefaultFlagsChange,
                label = { Text("Claude default flags") },
                placeholder = { Text("--permission-mode acceptEdits") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Flags are split like shell arguments and appended to one-tap Codex or Claude launches.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MetadataTransferCard(
    state: SettingsUiState,
    onExportMetadata: () -> Unit,
    onImportPayloadChange: (String) -> Unit,
    onImportMetadata: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Metadata transfer",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Export/import hosts and workspaces only. Passwords and private keys are never included.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onExportMetadata) {
                Text("Export metadata")
            }
            state.metadataExportPayload?.let { payload ->
                SelectionContainer {
                    Text(
                        text = payload,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            OutlinedTextField(
                value = state.importMetadataPayload,
                onValueChange = onImportPayloadChange,
                label = { Text("Paste metadata JSON") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = onImportMetadata) {
                Text("Import metadata")
            }
            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Privacy",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = PrivacyNotice,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
