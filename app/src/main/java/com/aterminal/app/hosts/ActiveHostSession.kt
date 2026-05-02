package com.aterminal.app.hosts

import com.aterminal.app.data.HostEntity
import com.aterminal.app.ssh.SshConnection
import com.aterminal.app.ssh.SshControlChannel
import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.ssh.SshPtyRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ActiveHostSession {
    val host: HostEntity
    val capabilities: RemoteCapabilities

    fun controlChannel(): SshControlChannel

    suspend fun openPty(request: SshPtyRequest = SshPtyRequest()): SshPtyChannel

    suspend fun disconnect()
}

class SshActiveHostSession(
    override val host: HostEntity,
    override val capabilities: RemoteCapabilities,
    private val connection: SshConnection,
) : ActiveHostSession {
    override fun controlChannel(): SshControlChannel {
        return connection.controlChannel()
    }

    override suspend fun openPty(request: SshPtyRequest): SshPtyChannel {
        return connection.openPty(request)
    }

    override suspend fun disconnect() {
        connection.disconnect()
    }
}

interface ActiveHostSessionSink {
    fun activate(session: ActiveHostSession)

    fun clear()
}

class ActiveHostSessionStore : ActiveHostSessionSink {
    private val mutableSession = MutableStateFlow<ActiveHostSession?>(null)
    val session: StateFlow<ActiveHostSession?> = mutableSession.asStateFlow()

    override fun activate(session: ActiveHostSession) {
        mutableSession.value = session
    }

    override fun clear() {
        mutableSession.value = null
    }
}
