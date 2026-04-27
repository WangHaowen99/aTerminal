package com.aterminal.app.ssh

import org.junit.Assert.assertEquals
import org.junit.Test

class SshClientFactoryTest {
    @Test
    fun createsConfiguredSshClient() {
        val factory = SshClientFactory(
            config = SshClientConfig(
                connectTimeoutMillis = 1_234,
                readTimeoutMillis = 5_678,
                keepAliveIntervalSeconds = 9,
            ),
        )

        val client = factory.createClient()

        assertEquals(1_234, client.connectTimeout)
        assertEquals(5_678, client.timeout)
        assertEquals(9, client.connection.keepAlive.keepAliveInterval)
    }
}
