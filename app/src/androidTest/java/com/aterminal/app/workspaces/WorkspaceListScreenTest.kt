package com.aterminal.app.workspaces

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aterminal.app.agents.AgentType
import com.aterminal.app.agents.WorkspaceAgentAction
import com.aterminal.app.data.WorkspaceEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkspaceListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsWorkspaceAgentActions() {
        composeRule.setContent {
            WorkspaceListScreen(
                workspaces = listOf(workspace()),
                onActionSelected = { _, _ -> },
            )
        }

        composeRule.onNodeWithText("aTerminal").assertIsDisplayed()
        composeRule.onNodeWithText("/srv/aTerminal").assertIsDisplayed()
        composeRule.onNodeWithText("Shell").assertIsDisplayed()
        composeRule.onNodeWithText("Start Codex").assertIsDisplayed()
        composeRule.onNodeWithText("Resume Codex Latest").assertIsDisplayed()
        composeRule.onNodeWithText("Start Claude").assertIsDisplayed()
        composeRule.onNodeWithText("Continue Claude").assertIsDisplayed()
        composeRule.onNodeWithText("Advanced").assertIsDisplayed()
    }

    @Test
    fun sendsSelectedPrimaryAction() {
        val selected = mutableListOf<String>()
        composeRule.setContent {
            WorkspaceListScreen(
                workspaces = listOf(workspace()),
                onActionSelected = { workspace, action ->
                    selected += "${workspace.name}:${action.label}"
                },
            )
        }

        composeRule.onNodeWithText("Start Codex").performClick()

        assertEquals(listOf("aTerminal:Start Codex"), selected)
    }

    @Test
    fun sendsResumeByIdActionFromAdvancedSheet() {
        val selected = mutableListOf<String>()
        composeRule.setContent {
            WorkspaceListScreen(
                workspaces = listOf(workspace()),
                onActionSelected = { workspace, action ->
                    selected += "${workspace.name}:${action.label}"
                },
            )
        }

        composeRule.onNodeWithText("Advanced").performClick()
        composeRule.onNodeWithText("Agent session id").performTextInput("codex-session-42")
        composeRule.onNodeWithText("Resume Codex by ID").performClick()

        assertEquals(listOf("aTerminal:Resume Codex by ID"), selected)
    }

    private fun workspace() = WorkspaceEntity(
        id = 7,
        hostId = 3,
        name = "aTerminal",
        remoteCwd = "/srv/aTerminal",
        preferredAgent = AgentType.CODEX,
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )
}
