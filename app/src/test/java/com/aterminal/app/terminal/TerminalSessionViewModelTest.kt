package com.aterminal.app.terminal

import com.aterminal.app.ssh.SshPtyChannel
import com.aterminal.app.ssh.SshPtySize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalSessionViewModelTest {
    @Test
    fun readsPtyOutputIntoTerminalBuffer() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = TerminalSessionViewModel(
            scope = TestScope(dispatcher),
            ioDispatcher = dispatcher,
        )

        viewModel.attach(pty(inputText = "hello\nworld"))
        advanceUntilIdle()

        assertEquals("hello\nworld", viewModel.state.value.screenText)
    }

    @Test
    fun ptyEndOfStreamReportsReconnectMessage() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = TerminalSessionViewModel(
            scope = TestScope(dispatcher),
            ioDispatcher = dispatcher,
        )

        viewModel.attach(pty(inputText = "partial output"))
        advanceUntilIdle()

        assertEquals(
            "SSH stream ended. Reconnect to reattach your tmux session.",
            viewModel.state.value.reconnectMessage,
        )
    }

    @Test
    fun writesUserInputAndQuickKeysToPty() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val remoteInput = ByteArrayOutputStream()
        val viewModel = TerminalSessionViewModel(
            scope = TestScope(dispatcher),
            ioDispatcher = dispatcher,
        )

        viewModel.attach(pty(remoteInput = remoteInput))
        viewModel.sendText("ls\n")
        viewModel.sendQuickKey(TerminalQuickKey.ControlC)
        advanceUntilIdle()

        assertEquals("ls\n\u0003", remoteInput.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun multilinePasteRequiresConfirmationBeforeWriting() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val remoteInput = ByteArrayOutputStream()
        val viewModel = TerminalSessionViewModel(
            scope = TestScope(dispatcher),
            ioDispatcher = dispatcher,
        )

        viewModel.attach(pty(remoteInput = remoteInput))
        viewModel.requestPaste("line one\nline two")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.pasteConfirmationRequired)
        assertEquals("line one\nline two", viewModel.state.value.pendingPasteText)
        assertEquals("", remoteInput.toString(Charsets.UTF_8.name()))

        viewModel.confirmPaste()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.pasteConfirmationRequired)
        assertNull(viewModel.state.value.pendingPasteText)
        assertEquals("line one\nline two", remoteInput.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun resizePropagatesToPty() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var requestedSize: SshPtySize? = null
        val viewModel = TerminalSessionViewModel(
            scope = TestScope(dispatcher),
            ioDispatcher = dispatcher,
        )

        viewModel.attach(
            pty(
                resize = { requestedSize = it },
            ),
        )
        viewModel.resize(columns = 120, rows = 40)
        advanceUntilIdle()

        assertEquals(SshPtySize(columns = 120, rows = 40), requestedSize)
    }

    private fun pty(
        inputText: String = "",
        remoteInput: ByteArrayOutputStream = ByteArrayOutputStream(),
        resize: suspend (SshPtySize) -> Unit = {},
    ): SshPtyChannel {
        return SshPtyChannel(
            input = ByteArrayInputStream(inputText.toByteArray(Charsets.UTF_8)),
            output = remoteInput,
            resize = resize,
            close = {},
        )
    }
}
