package com.aterminal.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
        onAppLanguageChange = viewModel::updateAppLanguage,
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
    onAppLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = settingsStrings(state.appLanguage)
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
            SettingsHeader(strings = strings)
            LanguageCard(
                appLanguage = state.appLanguage,
                strings = strings,
                onAppLanguageChange = onAppLanguageChange,
            )
            AgentDefaultsCard(
                codexDefaultFlags = state.codexDefaultFlags,
                claudeDefaultFlags = state.claudeDefaultFlags,
                strings = strings,
                onCodexDefaultFlagsChange = onCodexDefaultFlagsChange,
                onClaudeDefaultFlagsChange = onClaudeDefaultFlagsChange,
            )
            MetadataTransferCard(
                state = state,
                strings = strings,
                onExportMetadata = onExportMetadata,
                onImportPayloadChange = onImportPayloadChange,
                onImportMetadata = onImportMetadata,
            )
            PrivacyCard(strings = strings)
        }
    }
}

@Composable
private fun SettingsHeader(
    strings: SettingsStrings,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = strings.policyEyebrow,
            color = MaterialTheme.colorScheme.secondary,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = strings.settingsTitle,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = strings.settingsDescription,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun LanguageCard(
    appLanguage: AppLanguage,
    strings: SettingsStrings,
    onAppLanguageChange: (AppLanguage) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = strings.languageTitle,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = strings.languageDescription,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppLanguage.entries.forEach { language ->
                    if (language == appLanguage) {
                        Button(onClick = { onAppLanguageChange(language) }) {
                            Text(language.displayLabel)
                        }
                    } else {
                        OutlinedButton(onClick = { onAppLanguageChange(language) }) {
                            Text(language.displayLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentDefaultsCard(
    codexDefaultFlags: String,
    claudeDefaultFlags: String,
    strings: SettingsStrings,
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
                text = strings.agentDefaultsTitle,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = codexDefaultFlags,
                onValueChange = onCodexDefaultFlagsChange,
                label = { Text(strings.codexDefaultFlagsLabel) },
                placeholder = { Text("--model gpt-5.4") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = claudeDefaultFlags,
                onValueChange = onClaudeDefaultFlagsChange,
                label = { Text(strings.claudeDefaultFlagsLabel) },
                placeholder = { Text("--permission-mode acceptEdits") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = strings.agentDefaultsDescription,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MetadataTransferCard(
    state: SettingsUiState,
    strings: SettingsStrings,
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
                text = strings.metadataTransferTitle,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = strings.metadataTransferDescription,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onExportMetadata) {
                Text(strings.exportMetadata)
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
                label = { Text(strings.pasteMetadataJson) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = onImportMetadata) {
                Text(strings.importMetadata)
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
private fun PrivacyCard(
    strings: SettingsStrings,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = strings.privacyTitle,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = strings.privacyNotice,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
