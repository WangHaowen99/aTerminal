package com.aterminal.app.agents

import com.aterminal.app.hosts.RemoteCapabilities
import com.aterminal.app.hosts.RemoteToolCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceAgentActionAvailabilityTest {
    @Test
    fun enablesActionsWhenCapabilitiesAreUnknown() {
        assertTrue(WorkspaceAgentAction.StartCodex.availability(null).enabled)
        assertTrue(WorkspaceAgentAction.StartClaude.availability(null).enabled)
        assertTrue(WorkspaceAgentAction.Shell.availability(null).enabled)
    }

    @Test
    fun disablesAllWorkspaceActionsWhenTmuxIsMissing() {
        val capabilities = capabilities(
            tmux = false,
            codex = true,
            claude = true,
        )

        assertEquals(
            "Install tmux on the remote host before launching persistent agent sessions.",
            WorkspaceAgentAction.StartCodex.availability(capabilities).disabledReason,
        )
        assertEquals(
            "Install tmux on the remote host before launching persistent agent sessions.",
            WorkspaceAgentAction.Shell.availability(capabilities).disabledReason,
        )
    }

    @Test
    fun disablesOnlyActionsForMissingAgents() {
        val capabilities = capabilities(
            tmux = true,
            codex = false,
            claude = true,
        )

        assertEquals(
            "Codex is not installed on the remote host.",
            WorkspaceAgentAction.StartCodex.availability(capabilities).disabledReason,
        )
        assertEquals(
            "Codex is not installed on the remote host.",
            WorkspaceAgentAction.ResumeCodexLatest.availability(capabilities).disabledReason,
        )
        assertTrue(WorkspaceAgentAction.StartClaude.availability(capabilities).enabled)
        assertTrue(WorkspaceAgentAction.ContinueClaude.availability(capabilities).enabled)
        assertTrue(WorkspaceAgentAction.Shell.availability(capabilities).enabled)
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
}
