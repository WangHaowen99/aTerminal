package com.aterminal.app.hosts

import com.aterminal.app.data.HostEntity
import com.aterminal.app.ssh.SshAuthCredential
import com.aterminal.app.ssh.SshClientFactory
import com.aterminal.app.ssh.SshConnection
import com.aterminal.app.ssh.SshConnectionState

interface HostConnector {
    suspend fun connect(
        host: HostEntity,
        credential: SshAuthCredential,
    ): HostConnectionResult
}

data class HostConnectionResult(
    val capabilities: RemoteCapabilities,
)

class SshHostConnector(
    private val connectionFactory: () -> SshConnection = {
        SshConnection(SshClientFactory().createTransport())
    },
) : HostConnector {
    override suspend fun connect(
        host: HostEntity,
        credential: SshAuthCredential,
    ): HostConnectionResult {
        val connection = connectionFactory()
        connection.connect(host, credential)
        val state = connection.state.value
        check(state is SshConnectionState.Connected) {
            when (state) {
                is SshConnectionState.Failed -> state.message
                else -> "SSH connection did not reach connected state."
            }
        }
        val capabilities = RemoteCapabilityDetector(
            SshRemoteCapabilityChannel(connection.controlChannel()),
        ).detect()
        return HostConnectionResult(capabilities)
    }
}
