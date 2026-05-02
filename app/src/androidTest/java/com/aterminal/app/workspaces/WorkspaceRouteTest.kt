package com.aterminal.app.workspaces

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aterminal.app.agents.AgentSessionStore
import com.aterminal.app.agents.AgentType
import com.aterminal.app.data.AgentSessionEntity
import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.data.WorkspaceEntity
import com.aterminal.app.hosts.ActiveHostSession
import com.aterminal.app.hosts.RemoteCapabilities
import com.aterminal.app.hosts.RemoteToolCapability
import com.aterminal.app.ssh.SshAuthCredential
import com.aterminal.app.ssh.SshCommandResult
import com.aterminal.app.ssh.SshControlChannel
import com.aterminal.app.ssh.SshHostConfig
import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.ssh.SshPtyRequest
import com.aterminal.app.ssh.SshTransport
import com.aterminal.app.terminal.TerminalSessionSink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WorkspaceRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun activeSessionShowsHostWorkspacesAndLaunchesAgentIntoTerminal() {
        val transport = RecordingSshTransport()
        val remoteInput = ByteArrayOutputStream()
        val pty = pty(remoteInput)
        val terminalSink = RecordingTerminalSessionSink()
        var attachedCallbackCount = 0

        composeRule.setContent {
            WorkspaceRoute(
                activeSession = FakeActiveHostSession(
                    controlChannel = SshControlChannel(transport),
                    pty = pty,
                ),
                workspaceStore = FakeWorkspaceStore(listOf(workspace())),
                sessionStore = RecordingAgentSessionStore(),
                terminalSessionSink = terminalSink,
                onAttachedToTerminal = { attachedCallbackCount += 1 },
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("aTerminal").assertIsDisplayed()
        composeRule.onNodeWithText("Start Codex").performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(
                "tmux new-session -Ads 'aterm:codex:a-terminal' -c '/srv/aTerminal' 'codex --no-alt-screen'",
            ),
            transport.commands,
        )
        assertEquals(listOf(pty), terminalSink.attachedPtys)
        assertEquals(
            "tmux attach-session -t 'aterm:codex:a-terminal'\n",
            remoteInput.toString(Charsets.UTF_8.name()),
        )
        assertEquals(1, attachedCallbackCount)
    }

    private class FakeWorkspaceStore(
        private val workspaces: List<WorkspaceEntity>,
    ) : WorkspaceStore {
        override fun observeForHost(hostId: Long): Flow<List<WorkspaceEntity>> {
            return flowOf(workspaces.filter { it.hostId == hostId })
        }
    }

    private class RecordingAgentSessionStore : AgentSessionStore {
        override suspend fun saveLaunchedSession(session: AgentSessionEntity): Long = 91
    }

    private class RecordingTerminalSessionSink : TerminalSessionSink {
        val attachedPtys = mutableListOf<SshPtyChannel>()

        override fun attach(channel: SshPtyChannel) {
            attachedPtys += channel
        }
    }

    private class FakeActiveHostSession(
        private val controlChannel: SshControlChannel,
        private val pty: SshPtyChannel,
    ) : ActiveHostSession {
        override val host: HostEntity = HostEntity(
            id = 3,
            displayName = "Dev Box",
            hostname = "dev.example.com",
            port = 22,
            username = "agent",
            authType = AuthType.PRIVATE_KEY,
            createdAtEpochMillis = 100,
            updatedAtEpochMillis = 100,
        )
        override val capabilities: RemoteCapabilities = RemoteCapabilities(
            tmux = RemoteToolCapability(available = true),
            codex = RemoteToolCapability(available = true),
            claude = RemoteToolCapability(available = true),
        )

        override fun controlChannel(): SshControlChannel = controlChannel

        override suspend fun openPty(request: SshPtyRequest): SshPtyChannel = pty

        override suspend fun disconnect() = Unit
    }

    private class RecordingSshTransport : SshTransport {
        val commands = mutableListOf<String>()

        override suspend fun connect(host: SshHostConfig) = Unit

        override suspend fun authenticate(username: String, credential: SshAuthCredential) = Unit

        override suspend fun disconnect() = Unit

        override suspend fun execute(
            command: String,
            timeoutMillis: Long,
        ): SshCommandResult {
            commands += command
            return SshCommandResult(
                command = command,
                exitStatus = 0,
                stdout = "",
                stderr = "",
            )
        }

        override suspend fun openPty(request: SshPtyRequest): SshPtyChannel {
            error("Not used by this test.")
        }
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

    private fun pty(remoteInput: ByteArrayOutputStream): SshPtyChannel {
        return SshPtyChannel(
            input = ByteArrayInputStream(ByteArray(0)),
            output = remoteInput,
            resize = {},
            close = {},
        )
    }
}
