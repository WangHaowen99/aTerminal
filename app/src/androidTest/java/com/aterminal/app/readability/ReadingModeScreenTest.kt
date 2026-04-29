package com.aterminal.app.readability

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReadingModeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersReadableAgentBlocks() {
        composeRule.setContent {
            ReadingModeScreen(
                blocks = blocks(),
                onCopyRaw = {},
                onJumpToTerminal = {},
            )
        }

        composeRule.onNodeWithText("Plan").assertIsDisplayed()
        composeRule.onNodeWithText("Inspect files").assertIsDisplayed()
        composeRule.onNodeWithText("kotlin").assertIsDisplayed()
        composeRule.onNodeWithText("gradle test").assertIsDisplayed()
        composeRule.onNodeWithText("DIFF").assertIsDisplayed()
        composeRule.onNodeWithText("APPROVAL").assertIsDisplayed()
    }

    @Test
    fun collapsesLongOutputAndExpandsOnTap() {
        composeRule.setContent {
            ReadingModeScreen(
                blocks = listOf(
                    AgentOutputBlock.Output(
                        rawSource = longOutput(),
                        text = longOutput(),
                        collapsedByDefault = true,
                    ),
                ),
                onCopyRaw = {},
                onJumpToTerminal = {},
            )
        }

        composeRule.onNodeWithText("line 1").assertIsDisplayed()
        composeRule.onNodeWithText("line 8").assertDoesNotExist()

        composeRule.onNodeWithText("Show full output").performClick()

        composeRule.onNodeWithText("line 8").assertIsDisplayed()
    }

    @Test
    fun exposesCopyRawAndJumpToTerminalActions() {
        val copied = mutableListOf<String>()
        var jumped = false
        composeRule.setContent {
            ReadingModeScreen(
                blocks = listOf(
                    AgentOutputBlock.Command(
                        rawSource = "`$ gradle test`",
                        command = "gradle test",
                    ),
                ),
                onCopyRaw = { copied += it },
                onJumpToTerminal = { jumped = true },
            )
        }

        composeRule.onNodeWithText("Copy raw").performClick()
        composeRule.onNodeWithText("Jump to terminal").performClick()

        assertEquals(listOf("`$ gradle test`"), copied)
        assertEquals(true, jumped)
    }

    private fun blocks() = listOf(
        AgentOutputBlock.Heading(
            rawSource = "# Plan",
            level = 1,
            text = "Plan",
        ),
        AgentOutputBlock.ListBlock(
            rawSource = "- Inspect files",
            items = listOf("Inspect files"),
        ),
        AgentOutputBlock.Code(
            rawSource = "```kotlin\nprintln(\"ok\")\n```",
            language = "kotlin",
            code = "println(\"ok\")",
        ),
        AgentOutputBlock.Command(
            rawSource = "`$ gradle test`",
            command = "gradle test",
        ),
        AgentOutputBlock.Diff(
            rawSource = "diff --git a/a b/a\n-old\n+new",
            text = "diff --git a/a b/a\n-old\n+new",
        ),
        AgentOutputBlock.Approval(
            rawSource = "Approve? [y/N]",
            text = "Approve? [y/N]",
        ),
    )

    private fun longOutput() = (1..8).joinToString(separator = "\n") {
        "line $it"
    }
}
