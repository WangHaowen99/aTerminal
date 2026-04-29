package com.aterminal.app.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersAgentFlagControlsMetadataActionsAndPrivacyNote() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(
                    codexDefaultFlags = "",
                    claudeDefaultFlags = "",
                    metadataExportPayload = "{\"schemaVersion\":1}",
                ),
                onCodexDefaultFlagsChange = {},
                onClaudeDefaultFlagsChange = {},
                onExportMetadata = {},
                onImportPayloadChange = {},
                onImportMetadata = {},
            )
        }

        composeRule.onNodeWithText("Codex default flags").assertIsDisplayed()
        composeRule.onNodeWithText("Claude default flags").assertIsDisplayed()
        composeRule.onNodeWithText("Export metadata").assertIsDisplayed()
        composeRule.onNodeWithText("Import metadata").assertIsDisplayed()
        composeRule.onNodeWithText("{\"schemaVersion\":1}").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Terminal content stays on this device and the remote host unless you explicitly export logs or metadata.",
        ).assertIsDisplayed()
    }

    @Test
    fun exposesEditableImportPayloadField() {
        var payload = ""
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(importMetadataPayload = payload),
                onCodexDefaultFlagsChange = {},
                onClaudeDefaultFlagsChange = {},
                onExportMetadata = {},
                onImportPayloadChange = { payload = it },
                onImportMetadata = {},
            )
        }

        composeRule.onNodeWithText("Paste metadata JSON").performTextInput("{\"schemaVersion\":1}")
    }
}
