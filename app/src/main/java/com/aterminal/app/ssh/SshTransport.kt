package com.aterminal.app.ssh

interface SshTransport {
    suspend fun connect(host: SshHostConfig)

    suspend fun authenticate(username: String, credential: SshAuthCredential)

    suspend fun disconnect()

    suspend fun execute(command: String, timeoutMillis: Long): SshCommandResult

    suspend fun openPty(request: SshPtyRequest): SshPtyChannel
}
