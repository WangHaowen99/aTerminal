package com.aterminal.app.ssh

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SshPtyChannelTest {
    @Test
    fun writesUtf8InputToRemotePty() = runTest {
        val remoteInput = ByteArrayOutputStream()
        val channel = SshPtyChannel(
            input = ByteArrayInputStream(ByteArray(0)),
            output = remoteInput,
            resize = {},
            close = {},
        )

        channel.write("ls -la\n")

        assertEquals("ls -la\n", remoteInput.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun resizeAndCloseDelegateToRemoteChannel() = runTest {
        var requestedSize: SshPtySize? = null
        var closed = false
        val channel = SshPtyChannel(
            input = ByteArrayInputStream(ByteArray(0)),
            output = ByteArrayOutputStream(),
            resize = { requestedSize = it },
            close = { closed = true },
        )

        channel.resize(SshPtySize(columns = 100, rows = 32))
        channel.close()

        assertEquals(SshPtySize(columns = 100, rows = 32), requestedSize)
        assertTrue(closed)
    }
}
