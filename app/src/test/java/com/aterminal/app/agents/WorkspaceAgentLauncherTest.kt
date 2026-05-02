package com.aterminal.app.agents

import com.aterminal.app.data.AgentSessionEntity
import com.aterminal.app.data.WorkspaceEntity
import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.terminal.TerminalSessionSink
import com.aterminal.app.tmux.TmuxSession
import com.aterminal.app.tmux.TmuxSessionManager
import com.aterminal.app.tmux.TmuxTerminalAttacher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WorkspaceAgentLauncherTest {
    @Test
    fun launchingWorkspaceActionCreatesSessionStoresMetadataAndAttaches() = runTest {
        val executor = RecordingAgentLaunchExecutor()
        val sessionManager = RecordingTmuxSessionManager()
        val sessionStore = RecordingAgentSessionStore(savedId = 91)
        val launcher = WorkspaceAgentLauncher(
            launchExecutor = executor,
            sessionManager = sessionManager,
            sessionStore = sessionStore,
            clock = { 2_000L },
        )

        val result = launcher.launch(
            workspace = workspace(),
            action = WorkspaceAgentAction.StartCodex,
        )

        assertEquals(
            listOf(
                "tmux new-session -Ads 'aterm:codex:a-terminal' -c '/srv/aTerminal mobile' 'codex --no-alt-screen'",
            ),
            executor.commands,
        )
        assertEquals(listOf("aterm:codex:a-terminal"), sessionManager.attachedSessions)
        assertEquals(
            AgentSessionEntity(
                hostId = 3,
                workspaceId = 7,
                agentType = AgentType.CODEX,
                tmuxSessionName = "aterm:codex:a-terminal",
                agentSessionRef = null,
                lastAttachedAtEpochMillis = 2_000L,
            ),
            sessionStore.savedSessions.single(),
        )
        assertEquals(91, result.localSessionId)
        assertEquals("aterm:codex:a-terminal", result.tmuxSessionName)
    }

    @Test
    fun launchingWorkspaceActionCanAttachToTerminalPty() = runTest {
        val pty = pty()
        val terminalAttacher = RecordingTmuxTerminalAttacher(pty)
        val terminalSink = RecordingTerminalSessionSink()
        val sessionManager = RecordingTmuxSessionManager()
        val launcher = WorkspaceAgentLauncher(
            launchExecutor = RecordingAgentLaunchExecutor(),
            sessionManager = sessionManager,
            sessionStore = RecordingAgentSessionStore(savedId = 91),
            terminalAttacher = terminalAttacher,
            terminalSessionSink = terminalSink,
        )

        launcher.launch(
            workspace = workspace(),
            action = WorkspaceAgentAction.StartCodex,
        )

        assertEquals(listOf("aterm:codex:a-terminal"), terminalAttacher.attachedSessionNames)
        assertEquals(listOf(pty), terminalSink.attachedPtys)
        assertEquals(emptyList<String>(), sessionManager.attachedSessions)
    }

    @Test
    fun launchingAdvancedResumeStoresProvidedAgentSessionReference() = runTest {
        val sessionStore = RecordingAgentSessionStore(savedId = 92)
        val launcher = WorkspaceAgentLauncher(
            launchExecutor = RecordingAgentLaunchExecutor(),
            sessionManager = RecordingTmuxSessionManager(),
            sessionStore = sessionStore,
            clock = { 3_000L },
        )

        launcher.launch(
            workspace = workspace(),
            action = WorkspaceAgentAction.resumeClaudeById("claude session 42"),
        )

        val saved = sessionStore.savedSessions.single()
        assertEquals(AgentType.CLAUDE, saved.agentType)
        assertEquals("claude session 42", saved.agentSessionRef)
        assertEquals("aterm:claude:a-terminal:claude-session-42", saved.tmuxSessionName)
    }

    private class RecordingAgentLaunchExecutor : AgentLaunchExecutor {
        val commands = mutableListOf<String>()

        override suspend fun executeLaunchCommand(command: String) {
            commands += command
        }
    }

    private class RecordingAgentSessionStore(
        private val savedId: Long,
    ) : AgentSessionStore {
        val savedSessions = mutableListOf<AgentSessionEntity>()

        override suspend fun saveLaunchedSession(session: AgentSessionEntity): Long {
            savedSessions += session
            return savedId
        }
    }

    private class RecordingTmuxSessionManager : TmuxSessionManager {
        val attachedSessions = mutableListOf<String>()

        override suspend fun listSessions(): List<TmuxSession> = emptyList()

        override suspend fun attachSession(sessionName: String) {
            attachedSessions += sessionName
        }

        override suspend fun killSession(sessionName: String) = Unit

        override suspend fun detachClient() = Unit
    }

    private class RecordingTmuxTerminalAttacher(
        private val pty: SshPtyChannel,
    ) : TmuxTerminalAttacher {
        val attachedSessionNames = mutableListOf<String>()

        override suspend fun attach(sessionName: String): SshPtyChannel {
            attachedSessionNames += sessionName
            return pty
        }
    }

    private class RecordingTerminalSessionSink : TerminalSessionSink {
        val attachedPtys = mutableListOf<SshPtyChannel>()

        override fun attach(channel: SshPtyChannel) {
            attachedPtys += channel
        }
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

    private fun pty(): SshPtyChannel {
        return SshPtyChannel(
            input = ByteArrayInputStream(ByteArray(0)),
            output = ByteArrayOutputStream(),
            resize = {},
            close = {},
        )
    }
}
