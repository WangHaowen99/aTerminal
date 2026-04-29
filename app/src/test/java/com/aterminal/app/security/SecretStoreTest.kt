package com.aterminal.app.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class SecretStoreTest {
    @Test
    fun storesPrivateKeysOutsideRoomAsEncryptedPayloads() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SecretStore(
            context = context,
            cipher = InvertingSecretCipher(),
            preferencesName = "secret-store-test",
        )

        store.savePrivateKey(
            hostId = 42,
            privateKeyPem = "-----BEGIN PRIVATE KEY-----\nsecret\n-----END PRIVATE KEY-----",
        )

        assertEquals(
            "-----BEGIN PRIVATE KEY-----\nsecret\n-----END PRIVATE KEY-----",
            store.getPrivateKey(hostId = 42),
        )

        val storedPayload = context
            .getSharedPreferences("secret-store-test", Context.MODE_PRIVATE)
            .all
            .values
            .joinToString(separator = "\n")

        assertFalse(storedPayload.contains("BEGIN PRIVATE KEY"))
        assertFalse(storedPayload.contains("secret"))
    }

    @Test
    fun deletesPrivateKeyForHost() = runTest {
        val store = SecretStore(
            context = ApplicationProvider.getApplicationContext(),
            cipher = InvertingSecretCipher(),
            preferencesName = "secret-store-delete-test",
        )

        store.savePrivateKey(hostId = 7, privateKeyPem = "private-key")
        store.deletePrivateKey(hostId = 7)

        assertNull(store.getPrivateKey(hostId = 7))
    }

    private class InvertingSecretCipher : SecretCipher {
        override fun encrypt(plainText: ByteArray): EncryptedSecret {
            return EncryptedSecret(
                initializationVector = byteArrayOf(1, 2, 3),
                ciphertext = plainText.invert(),
            )
        }

        override fun decrypt(secret: EncryptedSecret): ByteArray {
            return secret.ciphertext.invert()
        }

        private fun ByteArray.invert(): ByteArray {
            return map { byte -> (byte.toInt() xor MASK).toByte() }.toByteArray()
        }
    }

    private companion object {
        const val MASK = 0x5A
    }
}
