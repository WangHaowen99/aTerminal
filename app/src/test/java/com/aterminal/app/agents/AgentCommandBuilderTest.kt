package com.aterminal.app.agents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AgentCommandBuilderTest {
    private val builder = AgentCommandBuilder()

    @Test
    fun buildsCodexStartCommandWithNoAltScreenByDefault() {
        val request = AgentLaunchRequest(
            agentType = AgentType.CODEX,
            workspaceCwd = "/srv/aTerminal mobile",
            tmuxSessionName = "aterm:codex:a-terminal",
        )

        assertEquals("codex --no-alt-screen", builder.buildAgentCommand(request))
        assertEquals(
            "tmux new-session -Ads 'aterm:codex:a-terminal' -c '/srv/aTerminal mobile' 'codex --no-alt-screen'",
            builder.buildLaunchCommand(request),
        )
    }

    @Test
    fun buildsCodexResumeLatestCommand() {
        val request = AgentLaunchRequest(
            agentType = AgentType.CODEX,
            mode = AgentLaunchMode.RESUME_LATEST,
            workspaceCwd = "/srv/aTerminal",
            tmuxSessionName = "aterm:codex:a-terminal:last",
        )

        assertEquals(
            "codex resume --no-alt-screen --last",
            builder.buildAgentCommand(request),
        )
    }

    @Test
    fun buildsCodexResumeByIdCommandWithQuotedSessionId() {
        val request = AgentLaunchRequest(
            agentType = AgentType.CODEX,
            mode = AgentLaunchMode.RESUME_BY_ID,
            workspaceCwd = "/srv/aTerminal",
            tmuxSessionName = "aterm:codex:a-terminal:abc123",
            agentSessionRef = "codex session 'alpha'",
        )

        assertEquals(
            "codex resume --no-alt-screen 'codex session '\"'\"'alpha'\"'\"''",
            builder.buildAgentCommand(request),
        )
    }

    @Test
    fun buildsClaudeStartContinueAndResumeCommands() {
        assertEquals(
            "claude",
            builder.buildAgentCommand(
                AgentLaunchRequest(
                    agentType = AgentType.CLAUDE,
                    workspaceCwd = "/srv/aTerminal",
                    tmuxSessionName = "aterm:claude:a-terminal",
                ),
            ),
        )
        assertEquals(
            "claude --continue",
            builder.buildAgentCommand(
                AgentLaunchRequest(
                    agentType = AgentType.CLAUDE,
                    mode = AgentLaunchMode.RESUME_LATEST,
                    workspaceCwd = "/srv/aTerminal",
                    tmuxSessionName = "aterm:claude:a-terminal:continue",
                ),
            ),
        )
        assertEquals(
            "claude --resume 'claude session 42'",
            builder.buildAgentCommand(
                AgentLaunchRequest(
                    agentType = AgentType.CLAUDE,
                    mode = AgentLaunchMode.RESUME_BY_ID,
                    workspaceCwd = "/srv/aTerminal",
                    tmuxSessionName = "aterm:claude:a-terminal:42",
                    agentSessionRef = "claude session 42",
                ),
            ),
        )
    }

    @Test
    fun quotesInitialPromptAndExtraFlagsAsUserProvidedArguments() {
        val request = AgentLaunchRequest(
            agentType = AgentType.CODEX,
            workspaceCwd = "/srv/aTerminal",
            tmuxSessionName = "aterm:codex:a-terminal",
            initialPrompt = "review the PR's risk",
            extraFlags = listOf("--model", "gpt-5.4"),
        )

        assertEquals(
            "codex --no-alt-screen '--model' 'gpt-5.4' 'review the PR'\"'\"'s risk'",
            builder.buildAgentCommand(request),
        )
    }

    @Test
    fun rejectsResumeByIdWithoutSessionRef() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            builder.buildAgentCommand(
                AgentLaunchRequest(
                    agentType = AgentType.CODEX,
                    mode = AgentLaunchMode.RESUME_BY_ID,
                    workspaceCwd = "/srv/aTerminal",
                    tmuxSessionName = "aterm:codex:a-terminal",
                ),
            )
        }

        assertEquals("agentSessionRef is required for resume by id.", error.message)
    }

    @Test
    fun exposesLowercaseAgentSlugsForTmuxSessionNames() {
        assertEquals("codex", AgentType.CODEX.remoteName)
        assertEquals("claude", AgentType.CLAUDE.remoteName)
        assertEquals("shell", AgentType.SHELL.remoteName)
    }
}
