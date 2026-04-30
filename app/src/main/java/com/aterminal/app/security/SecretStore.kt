package com.aterminal.app.security

import android.content.Context
import com.aterminal.app.data.AuthType
import java.nio.charset.StandardCharsets
import java.util.Base64

class SecretStore(
    context: Context,
    private val cipher: SecretCipher = AndroidKeystoreSecretCipher(),
    private val preferencesName: String = PREFERENCES_NAME,
) : HostCredentialStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        preferencesName,
        Context.MODE_PRIVATE,
    )

    suspend fun savePrivateKey(hostId: Long, privateKeyPem: String) {
        saveSecret(privateKeyPreferenceKey(hostId), privateKeyPem)
    }

    suspend fun getPrivateKey(hostId: Long): String? {
        return getSecret(privateKeyPreferenceKey(hostId))
    }

    suspend fun deletePrivateKey(hostId: Long) {
        deleteSecrets(
            privateKeyPreferenceKey(hostId),
            privateKeyPassphrasePreferenceKey(hostId),
        )
    }

    override suspend fun saveCredential(
        hostId: Long,
        credential: HostCredentialSecret,
    ) {
        when (credential) {
            is HostCredentialSecret.Password -> {
                saveSecret(passwordPreferenceKey(hostId), credential.password)
                deleteSecrets(
                    privateKeyPreferenceKey(hostId),
                    privateKeyPassphrasePreferenceKey(hostId),
                )
            }

            is HostCredentialSecret.PrivateKey -> {
                saveSecret(privateKeyPreferenceKey(hostId), credential.privateKeyPem)
                credential.passphrase?.let {
                    saveSecret(privateKeyPassphrasePreferenceKey(hostId), it)
                } ?: deleteSecrets(privateKeyPassphrasePreferenceKey(hostId))
                deleteSecrets(passwordPreferenceKey(hostId))
            }
        }
    }

    override suspend fun getCredential(
        hostId: Long,
        authType: AuthType,
    ): HostCredentialSecret? {
        return when (authType) {
            AuthType.PASSWORD -> getSecret(passwordPreferenceKey(hostId))
                ?.let(HostCredentialSecret::Password)

            AuthType.PRIVATE_KEY -> getPrivateKey(hostId)?.let { privateKey ->
                HostCredentialSecret.PrivateKey(
                    privateKeyPem = privateKey,
                    passphrase = getSecret(privateKeyPassphrasePreferenceKey(hostId)),
                )
            }
        }
    }

    override suspend fun deleteCredential(hostId: Long) {
        deleteSecrets(
            passwordPreferenceKey(hostId),
            privateKeyPreferenceKey(hostId),
            privateKeyPassphrasePreferenceKey(hostId),
        )
    }

    private fun saveSecret(key: String, value: String) {
        val encrypted = cipher.encrypt(value.toByteArray(StandardCharsets.UTF_8))
        val committed = preferences.edit()
            .putString(key, encode(encrypted))
            .commit()
        check(committed) { "Failed to persist host secret." }
    }

    private fun getSecret(key: String): String? {
        val encoded = preferences.getString(key, null) ?: return null
        val decrypted = cipher.decrypt(decode(encoded))
        return String(decrypted, StandardCharsets.UTF_8)
    }

    private fun deleteSecrets(vararg keys: String) {
        val committed = preferences.edit()
            .apply { keys.forEach(::remove) }
            .commit()
        check(committed) { "Failed to delete host secret." }
    }

    private fun passwordPreferenceKey(hostId: Long): String {
        require(hostId > 0) { "Host id must be persisted before storing secrets." }
        return "host.$hostId.password"
    }

    private fun privateKeyPreferenceKey(hostId: Long): String {
        require(hostId > 0) { "Host id must be persisted before storing secrets." }
        return "host.$hostId.private_key"
    }

    private fun privateKeyPassphrasePreferenceKey(hostId: Long): String {
        require(hostId > 0) { "Host id must be persisted before storing secrets." }
        return "host.$hostId.private_key_passphrase"
    }

    private fun encode(secret: EncryptedSecret): String {
        return listOf(
            encoder.encodeToString(secret.initializationVector),
            encoder.encodeToString(secret.ciphertext),
        ).joinToString(separator = PAYLOAD_SEPARATOR)
    }

    private fun decode(payload: String): EncryptedSecret {
        val parts = payload.split(PAYLOAD_SEPARATOR)
        require(parts.size == 2) { "Encrypted secret payload is malformed." }
        return EncryptedSecret(
            initializationVector = decoder.decode(parts[0]),
            ciphertext = decoder.decode(parts[1]),
        )
    }

    private companion object {
        const val PAYLOAD_SEPARATOR = ":"
        const val PREFERENCES_NAME = "aterminal_secrets"
        val decoder: Base64.Decoder = Base64.getDecoder()
        val encoder: Base64.Encoder = Base64.getEncoder()
    }
}
