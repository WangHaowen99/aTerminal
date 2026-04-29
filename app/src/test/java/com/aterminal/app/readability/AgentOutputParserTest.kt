package com.aterminal.app.readability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentOutputParserTest {
    private val parser = AgentOutputParser()

    @Test
    fun parsesMixedCodexMarkdownCodeCommandAndDiffBlocks() {
        val blocks = parser.parse(
            """
            # Plan

            - Inspect files
            - Patch parser

            `$ gradle test`

            ```kotlin
            fun main() {
                println("ok")
            }
            ```

            diff --git a/app.kt b/app.kt
            --- a/app.kt
            +++ b/app.kt
            @@ -1 +1 @@
            -old
            +new
            """.trimIndent(),
        )

        assertTrue(blocks[0] is AgentOutputBlock.Heading)
        assertEquals("Plan", (blocks[0] as AgentOutputBlock.Heading).text)
        assertTrue(blocks[1] is AgentOutputBlock.ListBlock)
        assertEquals(
            listOf("Inspect files", "Patch parser"),
            (blocks[1] as AgentOutputBlock.ListBlock).items,
        )
        assertTrue(blocks[2] is AgentOutputBlock.Command)
        assertEquals("gradle test", (blocks[2] as AgentOutputBlock.Command).command)
        assertTrue(blocks[3] is AgentOutputBlock.Code)
        assertEquals("kotlin", (blocks[3] as AgentOutputBlock.Code).language)
        assertTrue(blocks[4] is AgentOutputBlock.Diff)
        assertTrue(blocks[4].rawSource.contains("diff --git"))
    }

    @Test
    fun parsesClaudeApprovalAndLongOutputBlocks() {
        val longOutput = (1..12).joinToString(separator = "\n") {
            "line $it: build output"
        }
        val blocks = parser.parse(
            """
            Claude wants to run:
            rm -rf build
            Approve? [y/N]

            $longOutput
            """.trimIndent(),
        )

        assertTrue(blocks[0] is AgentOutputBlock.Approval)
        assertTrue((blocks[0] as AgentOutputBlock.Approval).text.contains("Approve?"))
        assertTrue(blocks[1] is AgentOutputBlock.Output)
        assertTrue((blocks[1] as AgentOutputBlock.Output).collapsedByDefault)
    }

    @Test
    fun sanitizesAnsiBeforeParsingAndKeepsRawSourceOnEveryBlock() {
        val blocks = parser.parse("\u001B[33m## Result\u001B[0m\n\nAll good")

        assertEquals(2, blocks.size)
        assertTrue(blocks.all { it.rawSource.isNotBlank() })
        assertTrue(blocks[0] is AgentOutputBlock.Heading)
        assertEquals("Result", (blocks[0] as AgentOutputBlock.Heading).text)
        assertTrue(blocks[1] is AgentOutputBlock.PlainText)
    }

    @Test
    fun fallsBackToUnknownForControlOnlyBlocksInsteadOfDroppingContent() {
        val blocks = parser.parse("\u001B[?25l")

        assertEquals(listOf(AgentOutputBlock.Unknown(rawSource = "\u001B[?25l", text = "")), blocks)
    }
}
