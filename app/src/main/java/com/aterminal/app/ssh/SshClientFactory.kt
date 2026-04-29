package com.aterminal.app.ssh

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

class SshClientFactory(
    private val config: SshClientConfig = SshClientConfig(),
) {
    fun createClient(): SSHClient {
        return SSHClient().apply {
            addHostKeyVerifier(config.hostKeyVerifier)
            connectTimeout = config.connectTimeoutMillis
            timeout = config.readTimeoutMillis
            connection.keepAlive.keepAliveInterval = config.keepAliveIntervalSeconds
        }
    }

    fun createTransport(): SshTransport {
        return SshjTransport(createClient())
    }
}

data class SshClientConfig(
    val connectTimeoutMillis: Int = 10_000,
    val readTimeoutMillis: Int = 30_000,
    val keepAliveIntervalSeconds: Int = 30,
    val hostKeyVerifier: HostKeyVerifier = PromiscuousVerifier(),
)
