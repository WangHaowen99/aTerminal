package com.aterminal.app.ssh

sealed interface SshAuthCredential {
    data class Password(
        val password: String,
    ) : SshAuthCredential

    data class PrivateKey(
        val privateKeyPem: String,
        val passphrase: String? = null,
    ) : SshAuthCredential
}
