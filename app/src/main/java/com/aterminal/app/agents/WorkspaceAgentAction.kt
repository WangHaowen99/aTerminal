package com.aterminal.app.agents

import com.aterminal.app.data.WorkspaceEntity
import com.aterminal.app.hosts.RemoteCapabilities
import com.aterminal.app.tmux.TmuxSessionName

sealed class WorkspaceAgentAction(
    val label: String,
    val agentType: AgentType,
    private val mode: AgentLaunchMode,
    private val sessionSuffix: String? = null,
    private val agentSessionRef: String? = null,
) {
    object Shell : WorkspaceAgentAction(
        label = "Shell",
        agentType = AgentType.SHELL,
        mode = AgentLaunchMode.START,
    )

    object StartCodex : WorkspaceAgentAction(
        label = "Start Codex",
        agentType = AgentType.CODEX,
        mode = AgentLaunchMode.START,
    )

    object ResumeCodexLatest : WorkspaceAgentAction(
        label = "Resume Codex Latest",
        agentType = AgentType.CODEX,
        mode = AgentLaunchMode.RESUME_LATEST,
        sessionSuffix = "last",
    )

    object StartClaude : WorkspaceAgentAction(
        label = "Start Claude",
        agentType = AgentType.CLAUDE,
        mode = AgentLaunchMode.START,
    )

    object ContinueClaude : WorkspaceAgentAction(
        label = "Continue Claude",
        agentType = AgentType.CLAUDE,
        mode = AgentLaunchMode.RESUME_LATEST,
        sessionSuffix = "continue",
    )

    private class ResumeById(
        label: String,
        agentType: AgentType,
        agentSessionRef: String,
    ) : WorkspaceAgentAction(
        label = label,
        agentType = agentType,
        mode = AgentLaunchMode.RESUME_BY_ID,
        sessionSuffix = agentSessionRef,
        agentSessionRef = agentSessionRef,
    )

    fun toLaunchRequest(
        workspace: WorkspaceEntity,
        initialPrompt: String? = null,
        extraFlags: List<String> = emptyList(),
    ): AgentLaunchRequest {
        return AgentLaunchRequest(
            agentType = agentType,
            mode = mode,
            workspaceCwd = workspace.remoteCwd,
            tmuxSessionName = TmuxSessionName.forWorkspace(
                agent = agentType.remoteName,
                workspaceName = workspace.name,
                suffix = sessionSuffix,
            ),
            agentSessionRef = agentSessionRef,
            initialPrompt = initialPrompt,
            extraFlags = extraFlags,
        )
    }

    fun availability(capabilities: RemoteCapabilities?): WorkspaceAgentActionAvailability {
        if (capabilities == null) {
            return WorkspaceAgentActionAvailability.Enabled
        }
        if (!capabilities.tmux.available) {
            return WorkspaceAgentActionAvailability.Disabled(
                capabilities.tmuxInstallGuidance
                    ?: "tmux is not installed on the remote host.",
            )
        }

        val agentAvailable = when (agentType) {
            AgentType.CODEX -> capabilities.codex.available
            AgentType.CLAUDE -> capabilities.claude.available
            AgentType.SHELL -> true
        }
        if (agentAvailable) {
            return WorkspaceAgentActionAvailability.Enabled
        }

        return WorkspaceAgentActionAvailability.Disabled(
            "${agentType.displayName} is not installed on the remote host.",
        )
    }

    companion object {
        val primaryActions: List<WorkspaceAgentAction>
            get() = listOf(
                Shell,
                StartCodex,
                ResumeCodexLatest,
                StartClaude,
                ContinueClaude,
            )

        fun resumeCodexById(sessionId: String): WorkspaceAgentAction {
            return resumeById(
                label = "Resume Codex by ID",
                agentType = AgentType.CODEX,
                sessionId = sessionId,
            )
        }

        fun resumeClaudeById(sessionId: String): WorkspaceAgentAction {
            return resumeById(
                label = "Resume Claude by ID",
                agentType = AgentType.CLAUDE,
                sessionId = sessionId,
            )
        }

        private fun resumeById(
            label: String,
            agentType: AgentType,
            sessionId: String,
        ): WorkspaceAgentAction {
            require(sessionId.isNotBlank()) {
                "session id is required."
            }
            return ResumeById(
                label = label,
                agentType = agentType,
                agentSessionRef = sessionId,
            )
        }
    }
}

sealed class WorkspaceAgentActionAvailability(
    val enabled: Boolean,
    val disabledReason: String?,
) {
    object Enabled : WorkspaceAgentActionAvailability(
        enabled = true,
        disabledReason = null,
    )

    class Disabled(reason: String) : WorkspaceAgentActionAvailability(
        enabled = false,
        disabledReason = reason,
    )
}
