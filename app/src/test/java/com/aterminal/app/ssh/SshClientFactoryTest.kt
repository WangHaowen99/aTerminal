package com.aterminal.app.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun createsAndroidSafeSshClientWithoutX25519KeyExchange() {
        val client = SshClientFactory().createClient()

        val keyExchangeNames = client.transport.config.keyExchangeFactories.map { it.name }

        assertFalse(
            "Android BC Provider does not reliably expose X25519.",
            keyExchangeNames.any { it.contains("curve25519") },
        )
        assertTrue(
            "SSH client still needs non-X25519 key exchange fallbacks.",
            keyExchangeNames.any { it.startsWith("diffie-hellman-") },
        )
    }
}
