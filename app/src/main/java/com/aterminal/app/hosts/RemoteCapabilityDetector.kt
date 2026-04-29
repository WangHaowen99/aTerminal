package com.aterminal.app.hosts

import com.aterminal.app.ssh.SshCommandResult
import com.aterminal.app.ssh.SshControlChannel

class RemoteCapabilityDetector(
    private val channel: RemoteCapabilityChannel,
) {
    suspend fun detect(): RemoteCapabilities {
        val tmux = detectCommand("tmux")
        val tmuxWithVersion = if (tmux.available) {
            tmux.copy(version = detectTmuxVersion())
        } else {
            tmux
        }

        return RemoteCapabilities(
            tmux = tmuxWithVersion,
            codex = detectCommand("codex"),
            claude = detectCommand("claude"),
        )
    }

    private suspend fun detectCommand(commandName: String): RemoteToolCapability {
        val result = channel.execute("command -v $commandName")
        val path = result.stdout.trim().takeIf {
            result.exitStatus == 0 && it.isNotBlank()
        }
        return RemoteToolCapability(
            available = path != null,
            path = path,
        )
    }

    private suspend fun detectTmuxVersion(): String? {
        val result = channel.execute("tmux -V")
        return result.stdout.trim().takeIf {
            result.exitStatus == 0 && it.isNotBlank()
        }
    }
}

interface RemoteCapabilityChannel {
    suspend fun execute(
        command: String,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): SshCommandResult

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
    }
}

class SshRemoteCapabilityChannel(
    private val controlChannel: SshControlChannel,
) : RemoteCapabilityChannel {
    override suspend fun execute(
        command: String,
        timeoutMillis: Long,
    ): SshCommandResult {
        return controlChannel.execute(command, timeoutMillis)
    }
}
