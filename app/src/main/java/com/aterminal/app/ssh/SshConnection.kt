package com.aterminal.app.ssh

import com.aterminal.app.data.HostEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SshConnection(
    private val transport: SshTransport,
) {
    private val mutableState = MutableStateFlow<SshConnectionState>(
        SshConnectionState.Disconnected,
    )
    val state: StateFlow<SshConnectionState> = mutableState.asStateFlow()

    suspend fun connect(host: HostEntity, credential: SshAuthCredential) {
        mutableState.value = SshConnectionState.Connecting
        runCatching {
            transport.connect(SshHostConfig.from(host))
            transport.authenticate(host.username, credential)
        }.onSuccess {
            mutableState.value = SshConnectionState.Connected
        }.onFailure { error ->
            disconnectAfterFailure()
            mutableState.value = SshConnectionState.Failed(
                message = error.message ?: error::class.java.simpleName,
                cause = error,
            )
        }
    }

    suspend fun disconnect() {
        transport.disconnect()
        mutableState.value = SshConnectionState.Disconnected
    }

    fun controlChannel(): SshControlChannel {
        return SshControlChannel(transport)
    }

    suspend fun openPty(request: SshPtyRequest = SshPtyRequest()): SshPtyChannel {
        return transport.openPty(request)
    }

    private suspend fun disconnectAfterFailure() {
        runCatching { transport.disconnect() }
    }
}
