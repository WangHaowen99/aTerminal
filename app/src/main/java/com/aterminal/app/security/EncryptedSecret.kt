package com.aterminal.app.security

data class EncryptedSecret(
    val initializationVector: ByteArray,
    val ciphertext: ByteArray,
)
