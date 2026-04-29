package com.aterminal.app.ssh

import com.aterminal.app.data.HostEntity
import com.aterminal.app.tmux.TmuxRepository
import com.aterminal.app.tmux.TmuxSession
import com.aterminal.app.tmux.TmuxSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReconnectCoordinator(
    private val connection: ReconnectConnection,
    private val lastSessionStore: LastAttachedTmuxSessionStore,
) {
    private val mutableState = MutableStateFlow<ReconnectUiState>(ReconnectUiState.Idle)
    val state: StateFlow<ReconnectUiState> = mutableState.asStateFlow()

    suspend fun recordAttachedSession(hostId: Long, sessionName: String) {
        lastSessionStore.saveLastAttachedSession(hostId, sessionName)
    }

    suspend fun onConnectionLost(hostId: Long, message: String) {
        mutableState.value = ReconnectUiState.Disconnected(
            message = message,
            lastSessionName = lastSessionStore.getLastAttachedSession(hostId),
        )
    }

    suspend fun reconnect(host: HostEntity, credential: SshAuthCredential) {
        mutableState.value = ReconnectUiState.Reconnecting
        runCatching {
            connection.connect(host, credential)
            restoreLastSession(host)
        }.onFailure { error ->
            mutableState.value = ReconnectUiState.Failed(
                message = error.message ?: error::class.java.simpleName,
            )
        }
    }

    private suspend fun restoreLastSession(host: HostEntity) {
        val manager = connection.tmuxSessionManager()
        val sessions = manager.listSessions()
        val lastSessionName = lastSessionStore.getLastAttachedSession(host.id)
        if (lastSessionName == null) {
            mutableState.value = ReconnectUiState.NeedsSessionSelection(
                message = "No previous tmux session is tracked for ${host.displayName}.",
                availableSessions = sessions,
                showWorkspaceActions = true,
            )
            return
        }

        val lastSessionStillExists = sessions.any { it.name == lastSessionName }
        if (lastSessionStillExists) {
            manager.attachSession(lastSessionName)
            mutableState.value = ReconnectUiState.Reattached(lastSessionName)
            return
        }

        mutableState.value = ReconnectUiState.NeedsSessionSelection(
            message = "Last tmux session $lastSessionName is no longer available.",
            availableSessions = sessions,
            showWorkspaceActions = true,
        )
    }
}

sealed interface ReconnectUiState {
    data object Idle : ReconnectUiState

    data object Reconnecting : ReconnectUiState

    data class Disconnected(
        val message: String,
        val lastSessionName: String?,
    ) : ReconnectUiState

    data class Reattached(
        val sessionName: String,
    ) : ReconnectUiState

    data class NeedsSessionSelection(
        val message: String,
        val availableSessions: List<TmuxSession>,
        val showWorkspaceActions: Boolean,
    ) : ReconnectUiState

    data class Failed(
        val message: String,
    ) : ReconnectUiState
}

interface ReconnectConnection {
    suspend fun connect(host: HostEntity, credential: SshAuthCredential)

    fun tmuxSessionManager(): TmuxSessionManager
}

class SshReconnectConnection(
    private val connection: SshConnection,
) : ReconnectConnection {
    override suspend fun connect(host: HostEntity, credential: SshAuthCredential) {
        connection.connect(host, credential)
        val state = connection.state.value
        check(state is SshConnectionState.Connected) {
            when (state) {
                is SshConnectionState.Failed -> state.message
                else -> "SSH reconnect did not reach connected state."
            }
        }
    }

    override fun tmuxSessionManager(): TmuxSessionManager {
        return TmuxRepository(connection.controlChannel())
    }
}
