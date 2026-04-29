package com.aterminal.app.agents

import com.aterminal.app.tmux.ShellQuoter
import com.aterminal.app.tmux.TmuxCommandBuilder

class AgentCommandBuilder(
    private val tmuxCommandBuilder: TmuxCommandBuilder = TmuxCommandBuilder(),
    private val quoter: (String) -> String = ShellQuoter::quote,
) {
    fun buildLaunchCommand(request: AgentLaunchRequest): String {
        return tmuxCommandBuilder.newSession(
            sessionName = request.tmuxSessionName,
            remoteCwd = request.workspaceCwd,
            command = buildAgentCommand(request),
        )
    }

    fun buildAgentCommand(request: AgentLaunchRequest): String {
        return when (request.agentType) {
            AgentType.CODEX -> buildCodexCommand(request)
            AgentType.CLAUDE -> buildClaudeCommand(request)
            AgentType.SHELL -> buildShellCommand(request)
        }
    }

    private fun buildCodexCommand(request: AgentLaunchRequest): String {
        val parts = when (request.mode) {
            AgentLaunchMode.START -> listOf("codex", "--no-alt-screen")
            AgentLaunchMode.RESUME_LATEST -> listOf(
                "codex",
                "resume",
                "--no-alt-screen",
                "--last",
            )
            AgentLaunchMode.RESUME_BY_ID -> listOf(
                "codex",
                "resume",
                "--no-alt-screen",
                requireSessionRef(request),
            )
        }
        return appendUserArgs(parts, request).joinToString(separator = " ")
    }

    private fun buildClaudeCommand(request: AgentLaunchRequest): String {
        val parts = when (request.mode) {
            AgentLaunchMode.START -> listOf("claude")
            AgentLaunchMode.RESUME_LATEST -> listOf("claude", "--continue")
            AgentLaunchMode.RESUME_BY_ID -> listOf(
                "claude",
                "--resume",
                requireSessionRef(request),
            )
        }
        return appendUserArgs(parts, request).joinToString(separator = " ")
    }

    private fun buildShellCommand(request: AgentLaunchRequest): String {
        require(request.mode == AgentLaunchMode.START) {
            "shell sessions do not support resume modes."
        }
        return "exec \${SHELL:-sh}"
    }

    private fun appendUserArgs(
        baseParts: List<String>,
        request: AgentLaunchRequest,
    ): List<String> {
        return buildList {
            addAll(baseParts)
            addAll(request.extraFlags.map(quoter))
            request.initialPrompt?.takeIf { it.isNotBlank() }?.let {
                add(quoter(it))
            }
        }
    }

    private fun requireSessionRef(request: AgentLaunchRequest): String {
        val sessionRef = request.agentSessionRef?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("agentSessionRef is required for resume by id.")
        return quoter(sessionRef)
    }
}
