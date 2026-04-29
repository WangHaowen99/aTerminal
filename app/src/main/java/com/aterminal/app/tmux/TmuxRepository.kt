package com.aterminal.app.tmux

import com.aterminal.app.errors.TmuxCommandFailedException
import com.aterminal.app.ssh.SshControlChannel

class TmuxRepository(
    private val controlChannel: TmuxControlChannel,
    private val commandBuilder: TmuxCommandBuilder = TmuxCommandBuilder(),
) : TmuxSessionManager {
    constructor(
        controlChannel: SshControlChannel,
        commandBuilder: TmuxCommandBuilder = TmuxCommandBuilder(),
    ) : this(SshTmuxControlChannel(controlChannel), commandBuilder)

    override suspend fun listSessions(): List<TmuxSession> {
        val result = controlChannel.execute(commandBuilder.listSessions())
        result.requireSuccess()
        return TmuxSessionParser.parseListSessions(result.stdout)
    }

    override suspend fun attachSession(sessionName: String) {
        controlChannel.execute(commandBuilder.attachSession(sessionName)).requireSuccess()
    }

    override suspend fun killSession(sessionName: String) {
        controlChannel.execute(commandBuilder.killSession(sessionName)).requireSuccess()
    }

    override suspend fun detachClient() {
        controlChannel.execute(commandBuilder.detachClient()).requireSuccess()
    }

    private fun com.aterminal.app.ssh.SshCommandResult.requireSuccess() {
        if (exitStatus != 0) {
            throw TmuxCommandFailedException(
                command = command,
                exitStatus = exitStatus,
                stderr = stderr,
            )
        }
    }
}

interface TmuxSessionManager {
    suspend fun listSessions(): List<TmuxSession>

    suspend fun attachSession(sessionName: String)

    suspend fun killSession(sessionName: String)

    suspend fun detachClient()
}
