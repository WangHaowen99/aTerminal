package com.aterminal.app.tmux

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.hosts.ActiveHostSession
import com.aterminal.app.hosts.RemoteCapabilities
import com.aterminal.app.hosts.RemoteToolCapability
import com.aterminal.app.ssh.SshControlChannel
import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.ssh.SshPtyRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TmuxPtyAttacherTest {
    @Test
    fun opensPtyAndWritesQuotedTmuxAttachCommand() = runTest {
        val remoteInput = ByteArrayOutputStream()
        val pty = SshPtyChannel(
            input = ByteArrayInputStream(ByteArray(0)),
            output = remoteInput,
            resize = {},
            close = {},
        )
        val activeSession = FakeActiveHostSession(pty)
        val attacher = TmuxPtyAttacher(activeSession)

        val attachedPty = attacher.attach("aterm:codex:backend api")

        assertSame(pty, attachedPty)
        assertEquals(1, activeSession.openPtyCount)
        assertEquals(
            "tmux attach-session -t 'aterm:codex:backend api'\n",
            remoteInput.toString(Charsets.UTF_8.name()),
        )
    }

    private class FakeActiveHostSession(
        private val pty: SshPtyChannel,
    ) : ActiveHostSession {
        var openPtyCount = 0

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
            openPtyCount += 1
            return pty
        }

        override suspend fun disconnect() = Unit
    }
}
