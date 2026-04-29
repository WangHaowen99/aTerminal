package com.aterminal.app.ssh

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SshConnectionTest {
    @Test
    fun connectReportsConnectingThenConnectedAndAuthenticates() = runTest {
        val transport = BlockingTransport()
        val connection = SshConnection(transport)
        val credential = SshAuthCredential.PrivateKey("private-key")

        val connectJob = launch {
            connection.connect(hostFixture(), credential)
        }
        transport.connectStarted.await()

        assertEquals(SshConnectionState.Connecting, connection.state.value)

        transport.allowConnect.complete(Unit)
        connectJob.join()

        assertEquals(SshConnectionState.Connected, connection.state.value)
        assertEquals(
            listOf(
                "connect:dev.example.com:2222",
                "auth:agent:PrivateKey",
            ),
            transport.events,
        )
    }

    @Test
    fun failedConnectReportsFailureAndDisconnects() = runTest {
        val transport = FailingTransport(error = IllegalStateException("network down"))
        val connection = SshConnection(transport)

        connection.connect(hostFixture(), SshAuthCredential.Password("password"))

        val state = connection.state.value
        assertTrue(state is SshConnectionState.Failed)
        assertEquals("network down", (state as SshConnectionState.Failed).message)
        assertTrue(transport.disconnected)
    }

    @Test
    fun disconnectClosesTransportAndReportsDisconnected() = runTest {
        val transport = BlockingTransport()
        val connection = SshConnection(transport)

        connection.disconnect()

        assertEquals(SshConnectionState.Disconnected, connection.state.value)
        assertTrue(transport.disconnected)
    }

    private fun hostFixture() = HostEntity(
        displayName = "Dev Box",
        hostname = "dev.example.com",
        port = 2222,
        username = "agent",
        authType = AuthType.PRIVATE_KEY,
        pinnedFingerprint = "SHA256:abc123",
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )

    private open class BlockingTransport : SshTransport {
        val connectStarted = CompletableDeferred<Unit>()
        val allowConnect = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        var disconnected = false

        override suspend fun connect(host: SshHostConfig) {
            events += "connect:${host.hostname}:${host.port}"
            connectStarted.complete(Unit)
            allowConnect.await()
        }

        override suspend fun authenticate(username: String, credential: SshAuthCredential) {
            events += "auth:$username:${credential::class.simpleName}"
        }

        override suspend fun disconnect() {
            disconnected = true
        }

        override suspend fun execute(command: String, timeoutMillis: Long): SshCommandResult {
            error("Not needed")
        }

        override suspend fun openPty(request: SshPtyRequest): SshPtyChannel {
            error("Not needed")
        }
    }

    private class FailingTransport(
        private val error: Throwable,
    ) : BlockingTransport() {
        override suspend fun connect(host: SshHostConfig) {
            throw error
        }
    }
}
