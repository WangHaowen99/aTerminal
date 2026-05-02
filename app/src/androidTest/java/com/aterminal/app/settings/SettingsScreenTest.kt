package com.aterminal.app.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
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
                onAppLanguageChange = {},
            )
        }

        composeRule.onNodeWithText("Language").assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("中文").assertIsDisplayed()
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
                onAppLanguageChange = {},
            )
        }

        composeRule.onNodeWithText("Paste metadata JSON").performTextInput("{\"schemaVersion\":1}")
    }

    @Test
    fun selectingChineseLanguageInvokesCallback() {
        val selectedLanguages = mutableListOf<AppLanguage>()
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(appLanguage = AppLanguage.ENGLISH),
                onCodexDefaultFlagsChange = {},
                onClaudeDefaultFlagsChange = {},
                onExportMetadata = {},
                onImportPayloadChange = {},
                onImportMetadata = {},
                onAppLanguageChange = { selectedLanguages += it },
            )
        }

        composeRule.onNodeWithText("中文").performClick()

        assertEquals(listOf(AppLanguage.CHINESE), selectedLanguages)
    }

    @Test
    fun rendersChineseSettingsLabelsWhenLanguageIsChinese() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(appLanguage = AppLanguage.CHINESE),
                onCodexDefaultFlagsChange = {},
                onClaudeDefaultFlagsChange = {},
                onExportMetadata = {},
                onImportPayloadChange = {},
                onImportMetadata = {},
                onAppLanguageChange = {},
            )
        }

        composeRule.onNodeWithText("设置").assertIsDisplayed()
        composeRule.onNodeWithText("语言").assertIsDisplayed()
        composeRule.onNodeWithText("Agent 默认参数").assertIsDisplayed()
        composeRule.onNodeWithText("导出元数据").assertIsDisplayed()
    }
}
