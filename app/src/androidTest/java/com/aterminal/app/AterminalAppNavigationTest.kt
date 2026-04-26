package com.aterminal.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AterminalAppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensHostListByDefault() {
        composeRule.onNodeWithText("Add a remote machine to start tmux-backed Codex and Claude Code sessions.")
            .assertIsDisplayed()
    }

    @Test
    fun navigatesToWorkspaceTerminalAndSettingsTabs() {
        composeRule.onNodeWithText("Workspaces").performClick()
        composeRule.onNodeWithText("Save project directories, then resume Codex or Claude Code in a few taps.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Terminal").performClick()
        composeRule.onNodeWithText("Interactive SSH output will appear here once a host or tmux session is attached.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Configure default agent flags, SSH behavior, and reading-mode preferences.")
            .assertIsDisplayed()
    }
}
