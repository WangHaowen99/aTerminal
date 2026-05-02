package com.aterminal.app.tmux

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.hosts.ActiveHostSession
import com.aterminal.app.hosts.RemoteCapabilities
import com.aterminal.app.hosts.RemoteToolCapability
import com.aterminal.app.ssh.SshControlChannel
import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.ssh.SshPtyRequest
import com.aterminal.app.terminal.TerminalSessionSink
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TmuxSessionRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun withoutActiveSessionShowsConnectPrompt() {
        composeRule.setContent {
            TmuxSessionRoute(activeSession = null)
        }

        composeRule.onNodeWithText("Connect to a host to list active tmux sessions.")
            .assertIsDisplayed()
    }

    @Test
    fun activeSessionLoadsTmuxSessions() {
        val manager = FakeTmuxSessionManager(
            sessions = listOf(session("aterm:codex:backend-api")),
        )

        composeRule.setContent {
            TmuxSessionRoute(
                activeSession = FakeActiveHostSession(),
                managerFactory = { manager },
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("aterm:codex:backend-api").assertIsDisplayed()
        assertEquals(1, manager.refreshCount)
    }

    @Test
    fun attachOpensPtyAndSendsItToTerminal() {
        val remoteInput = ByteArrayOutputStream()
        val pty = pty(remoteInput)
        val terminalSink = RecordingTerminalSessionSink()
        var attachedCallbackCount = 0

        composeRule.setContent {
            TmuxSessionRoute(
                activeSession = FakeActiveHostSession(pty),
                managerFactory = {
                    FakeTmuxSessionManager(
                        sessions = listOf(session("aterm:codex:backend-api")),
                    )
                },
                terminalSessionSink = terminalSink,
                onAttachedToTerminal = { attachedCallbackCount += 1 },
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Attach").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(pty), terminalSink.attachedPtys)
        assertEquals(
            "tmux attach-session -t 'aterm:codex:backend-api'\n",
            remoteInput.toString(Charsets.UTF_8.name()),
        )
        assertEquals(1, attachedCallbackCount)
    }

    @Test
    fun replacingActiveSessionForSameHostUsesNewManager() {
        val firstActiveSession = FakeActiveHostSession()
        val secondActiveSession = FakeActiveHostSession()
        val firstManager = FakeTmuxSessionManager(
            sessions = listOf(session("aterm:codex:first")),
        )
        val secondManager = FakeTmuxSessionManager(
            sessions = listOf(session("aterm:codex:second")),
        )
        var activeSession by mutableStateOf<ActiveHostSession>(firstActiveSession)

        composeRule.setContent {
            TmuxSessionRoute(
                activeSession = activeSession,
                managerFactory = { session ->
                    if (session === secondActiveSession) {
                        secondManager
                    } else {
                        firstManager
                    }
                },
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("aterm:codex:first").assertIsDisplayed()

        composeRule.runOnIdle {
            activeSession = secondActiveSession
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("aterm:codex:second").assertIsDisplayed()
        assertEquals(1, secondManager.refreshCount)
    }

    private class FakeTmuxSessionManager(
        private val sessions: List<TmuxSession>,
    ) : TmuxSessionManager {
        var refreshCount = 0

        override suspend fun listSessions(): List<TmuxSession> {
            refreshCount += 1
            return sessions
        }

        override suspend fun attachSession(sessionName: String) = Unit

        override suspend fun killSession(sessionName: String) = Unit

        override suspend fun detachClient() = Unit
    }

    private class RecordingTerminalSessionSink : TerminalSessionSink {
        val attachedPtys = mutableListOf<SshPtyChannel>()

        override fun attach(channel: SshPtyChannel) {
            attachedPtys += channel
        }
    }

    private class FakeActiveHostSession(
        private val pty: SshPtyChannel = pty(),
    ) : ActiveHostSession {
        override val host: HostEntity = HostEntity(
            id = 7,
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

        override fun controlChannel(): SshControlChannel {
            error("Not used by this test.")
        }

        override suspend fun openPty(request: SshPtyRequest): SshPtyChannel {
            return pty
        }

        override suspend fun disconnect() = Unit
    }

    private fun session(name: String) = TmuxSession(
        name = name,
        createdAtEpochSeconds = 1_710_000_000,
        attached = false,
        windowCount = 1,
    )

    private companion object {
        fun pty(remoteInput: ByteArrayOutputStream = ByteArrayOutputStream()): SshPtyChannel {
            return SshPtyChannel(
                input = ByteArrayInputStream(ByteArray(0)),
                output = remoteInput,
                resize = {},
                close = {},
            )
        }
    }
}
