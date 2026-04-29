package com.aterminal.app.tmux

import org.junit.Assert.assertEquals
import org.junit.Test

class TmuxCommandBuilderTest {
    private val builder = TmuxCommandBuilder()

    @Test
    fun buildsListSessionsCommand() {
        assertEquals(
            "tmux list-sessions -F '#{session_name}\\t#{session_created}\\t#{session_attached}\\t#{session_windows}'",
            builder.listSessions(),
        )
    }

    @Test
    fun buildsSessionLifecycleCommandsWithQuotedTargets() {
        assertEquals(
            "tmux has-session -t 'aterm:codex:backend-api'",
            builder.hasSession("aterm:codex:backend-api"),
        )
        assertEquals(
            "tmux attach-session -t 'aterm:codex:backend-api'",
            builder.attachSession("aterm:codex:backend-api"),
        )
        assertEquals(
            "tmux kill-session -t 'aterm:codex:backend-api'",
            builder.killSession("aterm:codex:backend-api"),
        )
        assertEquals("tmux detach-client", builder.detachClient())
    }

    @Test
    fun buildsAttachOrCreateNewSessionCommandWithQuotedCwdAndCommand() {
        assertEquals(
            "tmux new-session -As 'aterm:codex:a-terminal' -c '/srv/aTerminal mobile' 'codex --no-alt-screen'",
            builder.newSession(
                sessionName = "aterm:codex:a-terminal",
                remoteCwd = "/srv/aTerminal mobile",
                command = "codex --no-alt-screen",
            ),
        )
    }

    @Test
    fun quotesCommandPayloadContainingSingleQuotes() {
        assertEquals(
            "tmux new-session -As 'aterm:shell:quotes' -c '/tmp' 'printf '\"'\"'it works'\"'\"''",
            builder.newSession(
                sessionName = "aterm:shell:quotes",
                remoteCwd = "/tmp",
                command = "printf 'it works'",
            ),
        )
    }

    @Test
    fun buildsCapturePaneCommand() {
        assertEquals(
            "tmux capture-pane -t 'aterm:claude:backend-api' -p -J -S -3000",
            builder.capturePane(
                sessionName = "aterm:claude:backend-api",
                startLine = -3000,
            ),
        )
    }

    @Test
    fun generatesStableSessionNamesFromWorkspaceNames() {
        assertEquals(
            "aterm:codex:backend-api",
            TmuxSessionName.forWorkspace(agent = "codex", workspaceName = "Backend API"),
        )
        assertEquals(
            "aterm:claude:a-terminal",
            TmuxSessionName.forWorkspace(agent = "claude", workspaceName = "aTerminal 移动版"),
        )
        assertEquals(
            "aterm:shell:workspace",
            TmuxSessionName.forWorkspace(agent = "shell", workspaceName = "!!!"),
        )
        assertEquals(
            "aterm:codex:backend-api:abc123",
            TmuxSessionName.forWorkspace(
                agent = "codex",
                workspaceName = "Backend API",
                suffix = "abc123",
            ),
        )
    }
}
