package com.aterminal.app.ssh

sealed interface SshConnectionState {
    data object Disconnected : SshConnectionState

    data object Connecting : SshConnectionState

    data object Connected : SshConnectionState

    data class Failed(
        val message: String,
        val cause: Throwable? = null,
    ) : SshConnectionState
}
