package com.aterminal.app.tmux

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.hosts.ActiveHostSession
import com.aterminal.app.hosts.RemoteCapabilities
import com.aterminal.app.hosts.RemoteToolCapability
import com.aterminal.app.ssh.SshControlChannel
import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.ssh.SshPtyRequest
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TmuxSessionRouteKeyTest {
    @Test
    fun routeKeyChangesWhenSameHostGetsANewActiveSession() {
        val firstSession = FakeActiveHostSession()
        val secondSession = FakeActiveHostSession()

        assertNotEquals(
            tmuxSessionRouteKey(firstSession),
            tmuxSessionRouteKey(secondSession),
        )
    }

    private class FakeActiveHostSession : ActiveHostSession {
        override val host: HostEntity = HostEntity(
            id = 7,
            displayName = "Dev Box",
            hostname = "dev.example.com",
            port = 22,
            username = "agent",
            authType = AuthType.PRIVATE_KEY,
            createdAtEpochMillis = 100,
            updatedAtEpochMillis = 100,
        )
        override val capabilities: RemoteCapabilities = RemoteCapabilities(
            tmux = RemoteToolCapability(available = true),
            codex = RemoteToolCapability(available = true),
            claude = RemoteToolCapability(available = true),
        )

        override fun controlChannel(): SshControlChannel {
            error("Not used by this test.")
        }

        override suspend fun openPty(request: SshPtyRequest): SshPtyChannel {
            error("Not used by this test.")
        }

        override suspend fun disconnect() = Unit
    }
}
