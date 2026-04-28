package com.aterminal.app.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalBufferTest {
    @Test
    fun keepsVisibleScreenTextAndScrollback() {
        val buffer = TerminalBuffer(
            maxVisibleLines = 3,
            maxScrollbackLines = 2,
        )

        buffer.append("one\ntwo\nthree\nfour")

        assertEquals("two\nthree\nfour", buffer.snapshot().screenText)
        assertEquals(listOf("one"), buffer.snapshot().scrollbackLines)
    }

    @Test
    fun handlesCarriageReturnAndBackspaceForPromptUpdates() {
        val buffer = TerminalBuffer()

        buffer.append("progress 10%\rprogress 20%\nabc\b\bZ")

        assertEquals("progress 20%\naZ", buffer.snapshot().screenText)
    }

    @Test
    fun limitsScrollbackToConfiguredLineCount() {
        val buffer = TerminalBuffer(
            maxVisibleLines = 1,
            maxScrollbackLines = 2,
        )

        buffer.append("one\ntwo\nthree\nfour")

        assertEquals("four", buffer.snapshot().screenText)
        assertEquals(listOf("two", "three"), buffer.snapshot().scrollbackLines)
    }
}
