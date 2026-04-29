package com.aterminal.app.readability

import org.junit.Assert.assertEquals
import org.junit.Test

class AnsiSanitizerTest {
    @Test
    fun removesAnsiColorAndCursorControlsWhilePreservingReadableText() {
        val raw = "\u001B[32mPASS\u001B[0m \u001B[2Kline\u001B[1;5Hdone"

        assertEquals("PASS line done", AnsiSanitizer.sanitize(raw))
    }

    @Test
    fun removesOscTitleAndAltScreenControls() {
        val raw = "\u001B]0;codex\u0007\u001B[?1049hHello\u001B[?1049l"

        assertEquals("Hello", AnsiSanitizer.sanitize(raw))
    }

    @Test
    fun convertsCarriageReturnProgressIntoReadableLatestLine() {
        val raw = "Downloading 10%\rDownloading 80%\rDownloading 100%\nDone"

        assertEquals("Downloading 100%\nDone", AnsiSanitizer.sanitize(raw))
    }

    @Test
    fun preservesCodeIndentation() {
        val raw = "fun main() {\n    println(\"ok\")\n}"

        assertEquals(raw, AnsiSanitizer.sanitize(raw))
    }
}
