package com.aterminal.app.ssh

import com.aterminal.app.data.HostEntity

data class SshHostConfig(
    val hostname: String,
    val port: Int,
) {
    companion object {
        fun from(host: HostEntity): SshHostConfig {
            return SshHostConfig(
                hostname = host.hostname,
                port = host.port,
            )
        }
    }
}
