package com.aterminal.app.tmux

import com.aterminal.app.hosts.ActiveHostSession
import com.aterminal.app.ssh.SshPtyChannel

interface TmuxTerminalAttacher {
    suspend fun attach(sessionName: String): SshPtyChannel
}

class TmuxPtyAttacher(
    private val activeSession: ActiveHostSession,
    private val commandBuilder: TmuxCommandBuilder = TmuxCommandBuilder(),
) : TmuxTerminalAttacher {
    override suspend fun attach(sessionName: String): SshPtyChannel {
        val channel = activeSession.openPty()
        channel.write("${commandBuilder.attachSession(sessionName)}\n")
        return channel
    }
}
