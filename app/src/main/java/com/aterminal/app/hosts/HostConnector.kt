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
    )
}

class SshHostConnector(
    private val connectionFactory: () -> SshConnection = {
        SshConnection(SshClientFactory().createTransport())
    },
) : HostConnector {
    override suspend fun connect(
        host: HostEntity,
        credential: SshAuthCredential,
    ) {
        val connection = connectionFactory()
        connection.connect(host, credential)
        val state = connection.state.value
        check(state is SshConnectionState.Connected) {
            when (state) {
                is SshConnectionState.Failed -> state.message
                else -> "SSH connection did not reach connected state."
            }
        }
    }
}
