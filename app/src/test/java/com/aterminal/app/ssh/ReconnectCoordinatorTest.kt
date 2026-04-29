package com.aterminal.app.ssh

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.tmux.TmuxSession
import com.aterminal.app.tmux.TmuxSessionManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectCoordinatorTest {
    @Test
    fun tracksLastAttachedTmuxSessionPerHost() = runTest {
        val store = InMemoryLastAttachedTmuxSessionStore()
        val coordinator = ReconnectCoordinator(
            connection = RecordingReconnectConnection(),
            lastSessionStore = store,
        )

        coordinator.recordAttachedSession(hostId = 1, sessionName = "aterm:codex:a")
        coordinator.recordAttachedSession(hostId = 2, sessionName = "aterm:claude:b")

        assertEquals("aterm:codex:a", store.getLastAttachedSession(1))
        assertEquals("aterm:claude:b", store.getLastAttachedSession(2))
    }

    @Test
    fun reportsDisconnectedStateWithLastTrackedSession() = runTest {
        val store = InMemoryLastAttachedTmuxSessionStore()
        store.saveLastAttachedSession(hostId = 7, sessionName = "aterm:codex:a-terminal")
        val coordinator = ReconnectCoordinator(
            connection = RecordingReconnectConnection(),
            lastSessionStore = store,
        )

        coordinator.onConnectionLost(hostId = 7, message = "network lost")

        assertEquals(
            ReconnectUiState.Disconnected(
                message = "network lost",
                lastSessionName = "aterm:codex:a-terminal",
            ),
            coordinator.state.value,
        )
    }

    @Test
    fun reconnectsAndReattachesLastSessionWhenItStillExists() = runTest {
        val tmux = RecordingTmuxSessionManager(
            sessions = listOf(session("aterm:codex:a-terminal")),
        )
        val connection = RecordingReconnectConnection(tmux)
        val store = InMemoryLastAttachedTmuxSessionStore()
        store.saveLastAttachedSession(hostId = 7, sessionName = "aterm:codex:a-terminal")
        val coordinator = ReconnectCoordinator(
            connection = connection,
            lastSessionStore = store,
        )

        coordinator.reconnect(host(), SshAuthCredential.PrivateKey("pem"))

        assertEquals(listOf("connect:Dev Box:PrivateKey"), connection.events)
        assertEquals(listOf("aterm:codex:a-terminal"), tmux.attachedSessions)
        assertEquals(
            ReconnectUiState.Reattached("aterm:codex:a-terminal"),
            coordinator.state.value,
        )
    }

    @Test
    fun reconnectShowsSessionPickerAndWorkspaceActionsWhenLastSessionIsMissing() = runTest {
        val sessions = listOf(
            session("aterm:claude:a-terminal"),
            session("aterm:shell:a-terminal"),
        )
        val tmux = RecordingTmuxSessionManager(sessions = sessions)
        val store = InMemoryLastAttachedTmuxSessionStore()
        store.saveLastAttachedSession(hostId = 7, sessionName = "aterm:codex:missing")
        val coordinator = ReconnectCoordinator(
            connection = RecordingReconnectConnection(tmux),
            lastSessionStore = store,
        )

        coordinator.reconnect(host(), SshAuthCredential.PrivateKey("pem"))

        assertEquals(emptyList<String>(), tmux.attachedSessions)
        assertEquals(
            ReconnectUiState.NeedsSessionSelection(
                message = "Last tmux session aterm:codex:missing is no longer available.",
                availableSessions = sessions,
                showWorkspaceActions = true,
            ),
            coordinator.state.value,
        )
    }

    @Test
    fun reconnectFailureReportsFailedState() = runTest {
        val coordinator = ReconnectCoordinator(
            connection = RecordingReconnectConnection(
                connectError = IllegalStateException("auth failed"),
            ),
            lastSessionStore = InMemoryLastAttachedTmuxSessionStore(),
        )

        coordinator.reconnect(host(), SshAuthCredential.Password("password"))

        val state = coordinator.state.value
        assertTrue(state is ReconnectUiState.Failed)
        assertEquals("auth failed", (state as ReconnectUiState.Failed).message)
    }

    private class RecordingReconnectConnection(
        private val tmuxSessionManager: TmuxSessionManager = RecordingTmuxSessionManager(),
        private val connectError: Throwable? = null,
    ) : ReconnectConnection {
        val events = mutableListOf<String>()

        override suspend fun connect(host: HostEntity, credential: SshAuthCredential) {
            connectError?.let { throw it }
            events += "connect:${host.displayName}:${credential::class.simpleName}"
        }

        override fun tmuxSessionManager(): TmuxSessionManager {
            return tmuxSessionManager
        }
    }

    private class RecordingTmuxSessionManager(
        private val sessions: List<TmuxSession> = emptyList(),
    ) : TmuxSessionManager {
        val attachedSessions = mutableListOf<String>()

        override suspend fun listSessions(): List<TmuxSession> = sessions

        override suspend fun attachSession(sessionName: String) {
            attachedSessions += sessionName
        }

        override suspend fun killSession(sessionName: String) = Unit

        override suspend fun detachClient() = Unit
    }

    private fun host() = HostEntity(
        id = 7,
        displayName = "Dev Box",
        hostname = "dev.example.com",
        port = 22,
        username = "agent",
        authType = AuthType.PRIVATE_KEY,
        pinnedFingerprint = "SHA256:abc123",
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )

    private fun session(name: String) = TmuxSession(
        name = name,
        createdAtEpochSeconds = 1_710_000_000,
        attached = false,
        windowCount = 1,
    )
}
