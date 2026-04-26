package com.aterminal.app.security

interface SecretCipher {
    fun encrypt(plainText: ByteArray): EncryptedSecret

    fun decrypt(secret: EncryptedSecret): ByteArray
}
