package com.aterminal.app.agents

import com.aterminal.app.data.WorkspaceEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceAgentActionTest {
    @Test
    fun exposesPrimaryWorkspaceActionsInMobileOrder() {
        assertEquals(
            listOf(
                "Shell",
                "Start Codex",
                "Resume Codex Latest",
                "Start Claude",
                "Continue Claude",
            ),
            WorkspaceAgentAction.primaryActions.map { it.label },
        )
    }

    @Test
    fun buildsCodexStartRequestFromWorkspace() {
        val request = WorkspaceAgentAction.StartCodex.toLaunchRequest(workspace())

        assertEquals(AgentType.CODEX, request.agentType)
        assertEquals(AgentLaunchMode.START, request.mode)
        assertEquals("/srv/aTerminal mobile", request.workspaceCwd)
        assertEquals("aterm:codex:a-terminal", request.tmuxSessionName)
    }

    @Test
    fun buildsResumeLatestRequestsWithStableSessionSuffixes() {
        assertEquals(
            AgentLaunchRequest(
                agentType = AgentType.CODEX,
                mode = AgentLaunchMode.RESUME_LATEST,
                workspaceCwd = "/srv/aTerminal mobile",
                tmuxSessionName = "aterm:codex:a-terminal:last",
            ),
            WorkspaceAgentAction.ResumeCodexLatest.toLaunchRequest(workspace()),
        )
        assertEquals(
            AgentLaunchRequest(
                agentType = AgentType.CLAUDE,
                mode = AgentLaunchMode.RESUME_LATEST,
                workspaceCwd = "/srv/aTerminal mobile",
                tmuxSessionName = "aterm:claude:a-terminal:continue",
            ),
            WorkspaceAgentAction.ContinueClaude.toLaunchRequest(workspace()),
        )
    }

    @Test
    fun buildsAdvancedResumeByIdRequestsWithQuotedIdLeftToCommandBuilder() {
        val request = WorkspaceAgentAction.resumeCodexById("codex session 42")
            .toLaunchRequest(workspace())

        assertEquals(AgentType.CODEX, request.agentType)
        assertEquals(AgentLaunchMode.RESUME_BY_ID, request.mode)
        assertEquals("codex session 42", request.agentSessionRef)
        assertEquals("aterm:codex:a-terminal:codex-session-42", request.tmuxSessionName)
    }

    private fun workspace() = WorkspaceEntity(
        id = 7,
        hostId = 3,
        name = "aTerminal",
        remoteCwd = "/srv/aTerminal mobile",
        preferredAgent = AgentType.CODEX,
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )
}
