package com.aterminal.app.tmux

import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.terminal.TerminalSessionSink
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TmuxSessionListViewModelTest {
    @Test
    fun refreshLoadsSessionsIntoState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = FakeTmuxSessionManager(
            sessions = listOf(session("aterm:codex:backend-api")),
        )
        val viewModel = TmuxSessionListViewModel(
            manager = manager,
            scope = TestScope(dispatcher),
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(session("aterm:codex:backend-api")), viewModel.state.value.sessions)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun attachDelegatesToRepository() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = FakeTmuxSessionManager()
        val viewModel = TmuxSessionListViewModel(
            manager = manager,
            scope = TestScope(dispatcher),
        )

        viewModel.attachSession("aterm:codex:backend-api")
        advanceUntilIdle()

        assertEquals(listOf("aterm:codex:backend-api"), manager.attachedSessions)
    }

    @Test
    fun attachUsesTerminalAttacherWhenConfigured() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = FakeTmuxSessionManager()
        val pty = pty()
        val attacher = FakeTmuxTerminalAttacher(pty)
        val terminalSink = RecordingTerminalSessionSink()
        var attachedCallbackCount = 0
        val viewModel = TmuxSessionListViewModel(
            manager = manager,
            terminalAttacher = attacher,
            terminalSessionSink = terminalSink,
            onAttachedToTerminal = { attachedCallbackCount += 1 },
            scope = TestScope(dispatcher),
        )

        viewModel.attachSession("aterm:codex:backend-api")
        advanceUntilIdle()

        assertEquals(listOf("aterm:codex:backend-api"), attacher.attachedSessionNames)
        assertEquals(listOf(pty), terminalSink.attachedPtys)
        assertEquals(1, attachedCallbackCount)
        assertEquals(emptyList<String>(), manager.attachedSessions)
    }

    @Test
    fun killRequiresConfirmationBeforeRepositoryCall() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = FakeTmuxSessionManager()
        val viewModel = TmuxSessionListViewModel(
            manager = manager,
            scope = TestScope(dispatcher),
        )

        viewModel.requestKill("aterm:codex:backend-api")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.killConfirmationRequired)
        assertEquals("aterm:codex:backend-api", viewModel.state.value.pendingKillSessionName)
        assertEquals(emptyList<String>(), manager.killedSessions)

        viewModel.confirmKill()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.killConfirmationRequired)
        assertNull(viewModel.state.value.pendingKillSessionName)
        assertEquals(listOf("aterm:codex:backend-api"), manager.killedSessions)
    }

    @Test
    fun detachDelegatesToRepository() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = FakeTmuxSessionManager()
        val viewModel = TmuxSessionListViewModel(
            manager = manager,
            scope = TestScope(dispatcher),
        )

        viewModel.detachClient()
        advanceUntilIdle()

        assertEquals(1, manager.detachCount)
    }

    private class FakeTmuxSessionManager(
        private val sessions: List<TmuxSession> = emptyList(),
    ) : TmuxSessionManager {
        val attachedSessions = mutableListOf<String>()
        val killedSessions = mutableListOf<String>()
        var detachCount = 0

        override suspend fun listSessions(): List<TmuxSession> {
            return sessions
        }

        override suspend fun attachSession(sessionName: String) {
            attachedSessions += sessionName
        }

        override suspend fun killSession(sessionName: String) {
            killedSessions += sessionName
        }

        override suspend fun detachClient() {
            detachCount += 1
        }
    }

    private class FakeTmuxTerminalAttacher(
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

    private fun session(name: String) = TmuxSession(
        name = name,
        createdAtEpochSeconds = 1_710_000_000,
        attached = false,
        windowCount = 1,
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
