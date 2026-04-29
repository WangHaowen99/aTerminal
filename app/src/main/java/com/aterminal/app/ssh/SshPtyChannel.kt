package com.aterminal.app.ssh

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class SshPtyChannel(
    val input: InputStream,
    private val output: OutputStream,
    resize: suspend (SshPtySize) -> Unit,
    close: suspend () -> Unit,
) {
    private val closeRemote = close
    private val resizeRemote = resize

    suspend fun write(text: String) {
        write(text.toByteArray(StandardCharsets.UTF_8))
    }

    suspend fun write(bytes: ByteArray) {
        output.write(bytes)
        output.flush()
    }

    suspend fun resize(size: SshPtySize) {
        resizeRemote(size)
    }

    suspend fun close() {
        closeRemote()
    }
}

data class SshPtyRequest(
    val term: String = "xterm-256color",
    val columns: Int = 80,
    val rows: Int = 24,
    val widthPixels: Int = 0,
    val heightPixels: Int = 0,
)

data class SshPtySize(
    val columns: Int,
    val rows: Int,
    val widthPixels: Int = 0,
    val heightPixels: Int = 0,
)
