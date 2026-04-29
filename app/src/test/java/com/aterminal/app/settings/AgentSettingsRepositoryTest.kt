package com.aterminal.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.aterminal.app.agents.AgentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AgentSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun storesCodexAndClaudeDefaultFlags() = runTest {
        val repository = AgentSettingsRepository(dataStore(backgroundScope))

        repository.updateCodexDefaultFlags("--model gpt-5.4 --ask-for-approval never")
        repository.updateClaudeDefaultFlags("--permission-mode acceptEdits")

        assertEquals(
            AgentSettings(
                codexDefaultFlags = "--model gpt-5.4 --ask-for-approval never",
                claudeDefaultFlags = "--permission-mode acceptEdits",
            ),
            repository.settings.first(),
        )
    }

    @Test
    fun parsesDefaultFlagsForAgentLaunches() {
        val settings = AgentSettings(
            codexDefaultFlags = "--model 'gpt 5.4' --search",
            claudeDefaultFlags = "--permission-mode acceptEdits",
        )

        assertEquals(
            listOf("--model", "gpt 5.4", "--search"),
            settings.flagsFor(AgentType.CODEX),
        )
        assertEquals(
            listOf("--permission-mode", "acceptEdits"),
            settings.flagsFor(AgentType.CLAUDE),
        )
        assertEquals(emptyList<String>(), settings.flagsFor(AgentType.SHELL))
    }

    private fun dataStore(scope: CoroutineScope): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                temporaryFolder.newFile("agent-settings.preferences_pb")
            },
        )
    }
}
