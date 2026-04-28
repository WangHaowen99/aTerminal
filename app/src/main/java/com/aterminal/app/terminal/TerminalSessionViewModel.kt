package com.aterminal.app.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.ssh.SshPtySize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class TerminalSessionViewModel(
    private val buffer: TerminalBuffer = TerminalBuffer(),
    private val scope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TerminalUiState())
    val state: StateFlow<TerminalUiState> = mutableState.asStateFlow()

    private var readerJob: Job? = null
    private var ptyChannel: SshPtyChannel? = null
    private val terminalScope: CoroutineScope
        get() = scope ?: viewModelScope

    fun attach(channel: SshPtyChannel) {
        readerJob?.cancel()
        ptyChannel = channel
        readerJob = terminalScope.launch(ioDispatcher) {
            val chunk = ByteArray(READ_CHUNK_SIZE)
            while (true) {
                val count = channel.input.read(chunk)
                if (count < 0) {
                    break
                }
                appendOutput(String(chunk, 0, count, StandardCharsets.UTF_8))
            }
        }
    }

    fun sendText(text: String) {
        terminalScope.launch(ioDispatcher) {
            ptyChannel?.write(text)
        }
    }

    fun sendQuickKey(key: TerminalQuickKey) {
        sendText(key.sequence)
    }

    fun requestPaste(text: String) {
        if (text.contains('\n') || text.contains('\r')) {
            mutableState.update {
                it.copy(
                    pendingPasteText = text,
                    pasteConfirmationRequired = true,
                )
            }
            return
        }
        sendText(text)
    }

    fun confirmPaste() {
        val text = mutableState.value.pendingPasteText ?: return
        mutableState.update {
            it.copy(
                pendingPasteText = null,
                pasteConfirmationRequired = false,
            )
        }
        sendText(text)
    }

    fun cancelPaste() {
        mutableState.update {
            it.copy(
                pendingPasteText = null,
                pasteConfirmationRequired = false,
            )
        }
    }

    fun resize(columns: Int, rows: Int) {
        val size = SshPtySize(columns = columns, rows = rows)
        terminalScope.launch {
            ptyChannel?.resize(size)
        }
    }

    override fun onCleared() {
        readerJob?.cancel()
        terminalScope.launch {
            withContext(ioDispatcher) {
                ptyChannel?.close()
            }
        }
    }

    private fun appendOutput(text: String) {
        synchronized(buffer) {
            buffer.append(text)
            val snapshot = buffer.snapshot()
            mutableState.update {
                it.copy(
                    screenText = snapshot.screenText,
                    scrollbackLines = snapshot.scrollbackLines,
                )
            }
        }
    }

    private companion object {
        const val READ_CHUNK_SIZE = 4_096
    }
}

data class TerminalUiState(
    val screenText: String = "",
    val scrollbackLines: List<String> = emptyList(),
    val pendingPasteText: String? = null,
    val pasteConfirmationRequired: Boolean = false,
)
