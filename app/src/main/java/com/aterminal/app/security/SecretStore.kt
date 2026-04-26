package com.aterminal.app.security

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

class SecretStore(
    context: Context,
    private val cipher: SecretCipher = AndroidKeystoreSecretCipher(),
    private val preferencesName: String = PREFERENCES_NAME,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        preferencesName,
        Context.MODE_PRIVATE,
    )

    suspend fun savePrivateKey(hostId: Long, privateKeyPem: String) {
        val encrypted = cipher.encrypt(privateKeyPem.toByteArray(StandardCharsets.UTF_8))
        val committed = preferences.edit()
            .putString(privateKeyPreferenceKey(hostId), encode(encrypted))
            .commit()
        check(committed) { "Failed to persist private key secret." }
    }

    suspend fun getPrivateKey(hostId: Long): String? {
        val encoded = preferences.getString(privateKeyPreferenceKey(hostId), null)
            ?: return null
        val decrypted = cipher.decrypt(decode(encoded))
        return String(decrypted, StandardCharsets.UTF_8)
    }

    suspend fun deletePrivateKey(hostId: Long) {
        val committed = preferences.edit()
            .remove(privateKeyPreferenceKey(hostId))
            .commit()
        check(committed) { "Failed to delete private key secret." }
    }

    private fun privateKeyPreferenceKey(hostId: Long): String {
        require(hostId > 0) { "Host id must be persisted before storing secrets." }
        return "host.$hostId.private_key"
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
