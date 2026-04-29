package com.aterminal.app.terminal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TerminalReconnectBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsReconnectBannerAndDelegatesReconnectAction() {
        var reconnectClicks = 0
        composeRule.setContent {
            TerminalContent(
                state = TerminalUiState(
                    screenText = "agent output",
                    reconnectMessage = "Connection lost. Last session: aterm:codex:a-terminal",
                ),
                onInput = {},
                onPaste = {},
                onConfirmPaste = {},
                onCancelPaste = {},
                onQuickKey = {},
                onViewportResize = { _, _ -> },
                onDetach = {},
                onReconnect = { reconnectClicks += 1 },
            )
        }

        composeRule.onNodeWithText("Connection lost. Last session: aterm:codex:a-terminal")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Reconnect").performClick()

        assertEquals(1, reconnectClicks)
    }
}
