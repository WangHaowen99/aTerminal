package com.aterminal.app.terminal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class TerminalContentReadingModeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchesBetweenRawTerminalAndReadingMode() {
        composeRule.setContent {
            TerminalContent(
                state = TerminalUiState(
                    screenText = """
                    # Result

                    ```kotlin
                    println("ok")
                    ```
                    """.trimIndent(),
                ),
                onInput = {},
                onPaste = {},
                onConfirmPaste = {},
                onCancelPaste = {},
                onQuickKey = {},
                onViewportResize = { _, _ -> },
                onDetach = {},
            )
        }

        composeRule.onNodeWithText("Terminal").assertIsDisplayed()
        composeRule.onNodeWithText("Reading").performClick()

        composeRule.onNodeWithText("Reading mode").assertIsDisplayed()
        composeRule.onNodeWithText("Result").assertIsDisplayed()
        composeRule.onNodeWithText("kotlin").assertIsDisplayed()

        composeRule.onNodeWithText("Jump to terminal").performClick()

        composeRule.onNodeWithText("Send").assertIsDisplayed()
    }
}
