package com.aterminal.app.terminal

class TerminalBuffer(
    private val maxVisibleLines: Int = DEFAULT_VISIBLE_LINES,
    private val maxScrollbackLines: Int = DEFAULT_SCROLLBACK_LINES,
) {
    private val visibleLines = mutableListOf(StringBuilder())
    private val scrollbackLines = ArrayDeque<String>()
    private var cursorColumn = 0

    init {
        require(maxVisibleLines > 0) { "Visible line count must be positive." }
        require(maxScrollbackLines >= 0) { "Scrollback line count must not be negative." }
    }

    fun append(text: String) {
        text.forEach { char ->
            when (char) {
                '\n' -> appendNewline()
                '\r' -> clearCurrentLine()
                '\b' -> deletePreviousCharacter()
                else -> appendPrintable(char)
            }
        }
    }

    fun snapshot(): TerminalBufferSnapshot {
        return TerminalBufferSnapshot(
            screenText = visibleLines.joinToString(separator = "\n") { it.toString() },
            scrollbackLines = scrollbackLines.toList(),
        )
    }

    private fun appendPrintable(char: Char) {
        val line = visibleLines.last()
        if (cursorColumn < line.length) {
            line.setCharAt(cursorColumn, char)
        } else {
            line.append(char)
        }
        cursorColumn += 1
    }

    private fun appendNewline() {
        visibleLines += StringBuilder()
        cursorColumn = 0
        while (visibleLines.size > maxVisibleLines) {
            pushScrollback(visibleLines.removeAt(0).toString())
        }
    }

    private fun clearCurrentLine() {
        visibleLines.last().clear()
        cursorColumn = 0
    }

    private fun deletePreviousCharacter() {
        if (cursorColumn == 0) {
            return
        }
        val line = visibleLines.last()
        cursorColumn -= 1
        if (cursorColumn < line.length) {
            line.deleteCharAt(cursorColumn)
        }
    }

    private fun pushScrollback(line: String) {
        if (maxScrollbackLines == 0) {
            return
        }
        scrollbackLines.addLast(line)
        while (scrollbackLines.size > maxScrollbackLines) {
            scrollbackLines.removeFirst()
        }
    }

    private companion object {
        const val DEFAULT_SCROLLBACK_LINES = 2_000
        const val DEFAULT_VISIBLE_LINES = 80
    }
}

data class TerminalBufferSnapshot(
    val screenText: String,
    val scrollbackLines: List<String>,
)
