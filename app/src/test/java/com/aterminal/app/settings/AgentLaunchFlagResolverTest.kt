package com.aterminal.app.settings

import com.aterminal.app.agents.AgentType
import com.aterminal.app.agents.WorkspaceAgentAction
import com.aterminal.app.data.WorkspaceEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentLaunchFlagResolverTest {
    @Test
    fun combinesGlobalAgentDefaultsWithWorkspaceFlags() {
        val resolver = AgentLaunchFlagResolver(
            settings = AgentSettings(
                codexDefaultFlags = "--model gpt-5.4",
                claudeDefaultFlags = "--permission-mode acceptEdits",
            ),
        )

        assertEquals(
            listOf("--model", "gpt-5.4", "--search"),
            resolver.flagsFor(
                workspace = workspace(agentFlags = "--search"),
                action = WorkspaceAgentAction.StartCodex,
            ),
        )
        assertEquals(
            listOf("--permission-mode", "acceptEdits", "--verbose"),
            resolver.flagsFor(
                workspace = workspace(agentFlags = "--verbose"),
                action = WorkspaceAgentAction.StartClaude,
            ),
        )
    }

    @Test
    fun shellLaunchesIgnoreAgentDefaultsButKeepWorkspaceFlags() {
        val resolver = AgentLaunchFlagResolver(
            settings = AgentSettings(
                codexDefaultFlags = "--model gpt-5.4",
                claudeDefaultFlags = "--permission-mode acceptEdits",
            ),
        )

        assertEquals(
            listOf("--login"),
            resolver.flagsFor(
                workspace = workspace(agentFlags = "--login"),
                action = WorkspaceAgentAction.Shell,
            ),
        )
    }

    private fun workspace(agentFlags: String?) = WorkspaceEntity(
        id = 1,
        hostId = 2,
        name = "aTerminal",
        remoteCwd = "/srv/aterminal",
        preferredAgent = AgentType.CODEX,
        agentFlags = agentFlags,
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )
}
