package com.aterminal.app.ssh

class SshControlChannel(
    private val transport: SshTransport,
) {
    suspend fun execute(
        command: String,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): SshCommandResult {
        return transport.execute(command, timeoutMillis)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
    }
}
