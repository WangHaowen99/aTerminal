package com.aterminal.app.ssh

open class NoopTransport : SshTransport {
    override suspend fun connect(host: SshHostConfig) = Unit

    override suspend fun authenticate(username: String, credential: SshAuthCredential) = Unit

    override suspend fun disconnect() = Unit

    override suspend fun execute(command: String, timeoutMillis: Long): SshCommandResult {
        error("Not implemented")
    }

    override suspend fun openPty(request: SshPtyRequest): SshPtyChannel {
        error("Not implemented")
    }
}
