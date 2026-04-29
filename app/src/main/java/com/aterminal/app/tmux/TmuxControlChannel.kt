package com.aterminal.app.tmux

import com.aterminal.app.ssh.SshCommandResult
import com.aterminal.app.ssh.SshControlChannel

interface TmuxControlChannel {
    suspend fun execute(
        command: String,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): SshCommandResult

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
    }
}

class SshTmuxControlChannel(
    private val controlChannel: SshControlChannel,
) : TmuxControlChannel {
    override suspend fun execute(
        command: String,
        timeoutMillis: Long,
    ): SshCommandResult {
        return controlChannel.execute(command, timeoutMillis)
    }
}
