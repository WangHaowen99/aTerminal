package com.aterminal.app.ssh

import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

class SshClientFactory(
    private val config: SshClientConfig = SshClientConfig(),
) {
    fun createClient(): SSHClient {
        configureAndroidSecurityProvider()
        return SSHClient(createAndroidSafeSshjConfig()).apply {
            addHostKeyVerifier(config.hostKeyVerifier)
            connectTimeout = config.connectTimeoutMillis
            timeout = config.readTimeoutMillis
            connection.keepAlive.keepAliveInterval = config.keepAliveIntervalSeconds
        }
    }

    fun createTransport(): SshTransport {
        return SshjTransport(createClient())
    }

    private fun configureAndroidSecurityProvider() {
        SecurityUtils.setSecurityProvider(null)
        SecurityUtils.setRegisterBouncyCastle(false)
    }

    private fun createAndroidSafeSshjConfig(): DefaultConfig {
        return DefaultConfig().apply {
            setKeyExchangeFactories(
                keyExchangeFactories.filterNot { factory ->
                    factory.name in X25519_KEY_EXCHANGE_ALGORITHMS
                },
            )
        }
    }

    private companion object {
        val X25519_KEY_EXCHANGE_ALGORITHMS = setOf(
            "curve25519-sha256",
            "curve25519-sha256@libssh.org",
        )
    }
}

data class SshClientConfig(
    val connectTimeoutMillis: Int = 10_000,
    val readTimeoutMillis: Int = 30_000,
    val keepAliveIntervalSeconds: Int = 30,
    val hostKeyVerifier: HostKeyVerifier = PromiscuousVerifier(),
)
