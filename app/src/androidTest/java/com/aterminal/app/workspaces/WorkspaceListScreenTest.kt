package com.aterminal.app.workspaces

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aterminal.app.agents.AgentType
import com.aterminal.app.agents.WorkspaceAgentAction
import com.aterminal.app.data.WorkspaceEntity
import com.aterminal.app.hosts.RemoteCapabilities
import com.aterminal.app.hosts.RemoteToolCapability
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

    @Test
    fun disablesUnavailableAgentActionsWithClearMessages() {
        composeRule.setContent {
            WorkspaceListScreen(
                workspaces = listOf(workspace()),
                capabilities = capabilities(tmux = true, codex = false, claude = true),
                onActionSelected = { _, _ -> },
            )
        }

        composeRule.onNodeWithText("Start Codex").assertIsNotEnabled()
        composeRule.onNodeWithText(
            "Codex is not installed on the remote host. Install the codex CLI or choose Shell/Claude.",
        )
            .assertIsDisplayed()
        composeRule.onNodeWithText("Start Claude").assertIsDisplayed()
    }

    @Test
    fun showsTmuxInstallGuidanceWhenTmuxIsMissing() {
        composeRule.setContent {
            WorkspaceListScreen(
                workspaces = listOf(workspace()),
                capabilities = capabilities(tmux = false, codex = true, claude = true),
                onActionSelected = { _, _ -> },
            )
        }

        composeRule.onNodeWithText("Shell").assertIsNotEnabled()
        composeRule.onNodeWithText(
            "tmux is not installed on the remote host. Install tmux before launching persistent agent sessions.",
        ).assertIsDisplayed()
    }

    @Test
    fun disablesUnavailableAdvancedResumeActions() {
        composeRule.setContent {
            WorkspaceListScreen(
                workspaces = listOf(workspace()),
                capabilities = capabilities(tmux = true, codex = false, claude = true),
                onActionSelected = { _, _ -> },
            )
        }

        composeRule.onNodeWithText("Advanced").performClick()
        composeRule.onNodeWithText("Agent session id").performTextInput("session-42")

        composeRule.onNodeWithText("Resume Codex by ID").assertIsNotEnabled()
        composeRule.onNodeWithText("Resume Claude by ID").assertIsDisplayed()
    }

    private fun capabilities(
        tmux: Boolean,
        codex: Boolean,
        claude: Boolean,
    ) = RemoteCapabilities(
        tmux = capability(tmux),
        codex = capability(codex),
        claude = capability(claude),
    )

    private fun capability(available: Boolean) = RemoteToolCapability(
        available = available,
        path = if (available) "/usr/bin/tool" else null,
    )

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
