package com.aterminal.app.ssh

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SshControlChannelTest {
    @Test
    fun executesNonInteractiveCommandThroughTransport() = runTest {
        val transport = RecordingTransport(
            result = SshCommandResult(
                command = "echo ok",
                exitStatus = 0,
                stdout = "ok\n",
                stderr = "",
            ),
        )
        val channel = SshControlChannel(transport)

        val result = channel.execute("echo ok", timeoutMillis = 2_000)

        assertEquals("echo ok", transport.executedCommand)
        assertEquals(2_000L, transport.executedTimeoutMillis)
        assertEquals(0, result.exitStatus)
        assertEquals("ok\n", result.stdout)
    }

    private class RecordingTransport(
        private val result: SshCommandResult,
    ) : NoopTransport() {
        var executedCommand: String? = null
        var executedTimeoutMillis: Long? = null

        override suspend fun execute(command: String, timeoutMillis: Long): SshCommandResult {
            executedCommand = command
            executedTimeoutMillis = timeoutMillis
            return result
        }
    }
}
