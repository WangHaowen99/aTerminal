package com.aterminal.app.hosts

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.ssh.SshControlChannel
import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.ssh.SshPtyRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveHostSessionStoreTest {
    @Test
    fun activatePublishesActiveSessionAndClearRemovesIt() {
        val store = ActiveHostSessionStore()
        val session = FakeActiveHostSession(host = host())

        store.activate(session)

        assertEquals(session, store.session.value)

        store.clear()

        assertNull(store.session.value)
    }

    private class FakeActiveHostSession(
        override val host: HostEntity,
    ) : ActiveHostSession {
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

    private fun host() = HostEntity(
        id = 7,
        displayName = "Dev Box",
        hostname = "dev.example.com",
        port = 22,
        username = "agent",
        authType = AuthType.PRIVATE_KEY,
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )
}
