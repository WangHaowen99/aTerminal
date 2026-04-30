package com.aterminal.app.security

import com.aterminal.app.data.AuthType

interface HostCredentialStore {
    suspend fun saveCredential(
        hostId: Long,
        credential: HostCredentialSecret,
    )

    suspend fun getCredential(
        hostId: Long,
        authType: AuthType,
    ): HostCredentialSecret?

    suspend fun deleteCredential(hostId: Long)
}

sealed interface HostCredentialSecret {
    data class Password(
        val password: String,
    ) : HostCredentialSecret

    data class PrivateKey(
        val privateKeyPem: String,
        val passphrase: String? = null,
    ) : HostCredentialSecret
}
