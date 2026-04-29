package com.aterminal.app.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalQuickKeyTest {
    @Test
    fun exposesExpectedControlSequences() {
        assertEquals("\u001B", TerminalQuickKey.Escape.sequence)
        assertEquals("\t", TerminalQuickKey.Tab.sequence)
        assertEquals("\u001B[A", TerminalQuickKey.ArrowUp.sequence)
        assertEquals("\u001B[B", TerminalQuickKey.ArrowDown.sequence)
        assertEquals("\u001B[D", TerminalQuickKey.ArrowLeft.sequence)
        assertEquals("\u001B[C", TerminalQuickKey.ArrowRight.sequence)
        assertEquals("\u0003", TerminalQuickKey.ControlC.sequence)
        assertEquals("\u0004", TerminalQuickKey.ControlD.sequence)
        assertEquals("\u0002", TerminalQuickKey.TmuxPrefix.sequence)
    }
}
