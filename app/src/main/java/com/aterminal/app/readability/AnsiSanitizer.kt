package com.aterminal.app.readability

object AnsiSanitizer {
    fun sanitize(raw: String): String {
        val withoutOsc = raw.replace(OSC_SEQUENCE, "")
        val withoutCsi = withoutOsc.replace(CSI_SEQUENCE, " ")
        val withoutEsc = withoutCsi.replace(ESC_SEQUENCE, "")
        return normalizeCarriageReturns(withoutEsc)
            .lines()
            .joinToString(separator = "\n") { it.trimEnd().collapseAnsiGaps() }
            .trim()
    }

    private fun normalizeCarriageReturns(value: String): String {
        return value
            .split('\n')
            .joinToString(separator = "\n") { line ->
                line.split('\r').last()
            }
    }

    private val OSC_SEQUENCE = Regex("\u001B\\].*?(?:\u0007|\u001B\\\\)")
    private val CSI_SEQUENCE = Regex("\u001B\\[[0-?]*[ -/]*[@-~]")
    private val ESC_SEQUENCE = Regex("\u001B.")
    private val ANSI_GAP = Regex("(?<=\\S) {2,}(?=\\S)")

    private fun String.collapseAnsiGaps(): String {
        return replace(ANSI_GAP, " ")
    }
}
