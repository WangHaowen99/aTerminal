package com.aterminal.app.hosts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HostListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateShowsAddHostAction() {
        composeRule.setContent {
            HostListScreen(
                state = HostListUiState(),
                onAddHostClick = {},
                onDismissAddHost = {},
                onFormChange = {},
                onSaveHost = {},
                onDeleteHost = {},
                onConnectHost = {},
            )
        }

        composeRule.onNodeWithText("Hosts").assertIsDisplayed()
        composeRule.onNodeWithText("Add a remote machine to start tmux-backed Codex and Claude Code sessions.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Add host").assertIsDisplayed()
    }

    @Test
    fun rendersSavedHostsWithConnectAndDeleteActions() {
        composeRule.setContent {
            HostListScreen(
                state = HostListUiState(
                    hosts = listOf(host()),
                ),
                onAddHostClick = {},
                onDismissAddHost = {},
                onFormChange = {},
                onSaveHost = {},
                onDeleteHost = {},
                onConnectHost = {},
            )
        }

        composeRule.onNodeWithText("Dev Box").assertIsDisplayed()
        composeRule.onNodeWithText("agent@dev.example.com:2222").assertIsDisplayed()
        composeRule.onNodeWithText("Private key").assertIsDisplayed()
        composeRule.onNodeWithText("Connect").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun sendsConnectActionForSavedHost() {
        val connectedHostIds = mutableListOf<Long>()
        composeRule.setContent {
            HostListScreen(
                state = HostListUiState(
                    hosts = listOf(host()),
                ),
                onAddHostClick = {},
                onDismissAddHost = {},
                onFormChange = {},
                onSaveHost = {},
                onDeleteHost = {},
                onConnectHost = { connectedHostIds += it.id },
            )
        }

        composeRule.onNodeWithText("Connect").performClick()

        assertEquals(listOf(7L), connectedHostIds)
    }

    @Test
    fun showsConnectionStatus() {
        composeRule.setContent {
            HostListScreen(
                state = HostListUiState(
                    hosts = listOf(host()),
                    connectingHostId = 7,
                    statusMessage = "Connecting to Dev Box...",
                ),
                onAddHostClick = {},
                onDismissAddHost = {},
                onFormChange = {},
                onSaveHost = {},
                onDeleteHost = {},
                onConnectHost = {},
            )
        }

        composeRule.onNodeWithText("Connecting to Dev Box...").assertIsDisplayed()
        composeRule.onNodeWithText("Connecting").assertIsDisplayed()
    }

    @Test
    fun submitsAddHostForm() {
        var saveCount = 0
        lateinit var latestForm: HostFormState

        composeRule.setContent {
            var form by remember { mutableStateOf(HostFormState()) }
            latestForm = form
            HostListScreen(
                state = HostListUiState(
                    form = form,
                    showAddHostDialog = true,
                ),
                onAddHostClick = {},
                onDismissAddHost = {},
                onFormChange = {
                    form = it
                    latestForm = it
                },
                onSaveHost = { saveCount += 1 },
                onDeleteHost = {},
                onConnectHost = {},
            )
        }

        composeRule.onNodeWithText("Display name").performTextInput("Dev Box")
        composeRule.onNodeWithText("Hostname").performTextInput("dev.example.com")
        composeRule.onNodeWithText("Port").performTextClearance()
        composeRule.onNodeWithText("Port").performTextInput("2222")
        composeRule.onNodeWithText("Username").performTextInput("agent")
        composeRule.onNodeWithText("Private key PEM").performTextInput("private-key")
        composeRule.onNodeWithText("Save host").performClick()

        assertEquals(
            HostFormState(
                displayName = "Dev Box",
                hostname = "dev.example.com",
                portText = "2222",
                username = "agent",
                authType = AuthType.PRIVATE_KEY,
                privateKeyPem = "private-key",
            ),
            latestForm,
        )
        assertEquals(1, saveCount)
    }

    @Test
    fun showsCredentialFieldForSelectedAuthType() {
        composeRule.setContent {
            HostListScreen(
                state = HostListUiState(
                    form = HostFormState(authType = AuthType.PASSWORD),
                    showAddHostDialog = true,
                ),
                onAddHostClick = {},
                onDismissAddHost = {},
                onFormChange = {},
                onSaveHost = {},
                onDeleteHost = {},
                onConnectHost = {},
            )
        }

        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("SSH password").assertIsDisplayed()
    }

    private fun host() = HostEntity(
        id = 7,
        displayName = "Dev Box",
        hostname = "dev.example.com",
        port = 2222,
        username = "agent",
        authType = AuthType.PRIVATE_KEY,
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )
}
