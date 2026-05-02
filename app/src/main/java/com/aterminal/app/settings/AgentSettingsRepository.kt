package com.aterminal.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aterminal.app.agents.AgentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AgentSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AgentSettings> = dataStore.data.map { preferences ->
        AgentSettings(
            codexDefaultFlags = preferences[CODEX_DEFAULT_FLAGS].orEmpty(),
            claudeDefaultFlags = preferences[CLAUDE_DEFAULT_FLAGS].orEmpty(),
            appLanguage = AppLanguage.fromStorageValue(preferences[APP_LANGUAGE]),
        )
    }

    suspend fun updateCodexDefaultFlags(flags: String) {
        dataStore.edit { preferences ->
            preferences[CODEX_DEFAULT_FLAGS] = flags
        }
    }

    suspend fun updateClaudeDefaultFlags(flags: String) {
        dataStore.edit { preferences ->
            preferences[CLAUDE_DEFAULT_FLAGS] = flags
        }
    }

    suspend fun updateAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language.storageValue
        }
    }

    private companion object {
        val CODEX_DEFAULT_FLAGS = stringPreferencesKey("codex_default_flags")
        val CLAUDE_DEFAULT_FLAGS = stringPreferencesKey("claude_default_flags")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }
}

data class AgentSettings(
    val codexDefaultFlags: String = "",
    val claudeDefaultFlags: String = "",
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
) {
    fun flagsFor(agentType: AgentType): List<String> {
        return when (agentType) {
            AgentType.CODEX -> AgentFlagParser.parse(codexDefaultFlags)
            AgentType.CLAUDE -> AgentFlagParser.parse(claudeDefaultFlags)
            AgentType.SHELL -> emptyList()
        }
    }
}

object AgentFlagParser {
    fun parse(rawFlags: String?): List<String> {
        val raw = rawFlags.orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }

        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaping = false
        var tokenStarted = false

        raw.forEach { char ->
            when {
                escaping -> {
                    current.append(char)
                    escaping = false
                    tokenStarted = true
                }

                char == '\\' -> {
                    escaping = true
                    tokenStarted = true
                }

                quote != null -> {
                    if (char == quote) {
                        quote = null
                    } else {
                        current.append(char)
                    }
                    tokenStarted = true
                }

                char == '\'' || char == '"' -> {
                    quote = char
                    tokenStarted = true
                }

                char.isWhitespace() -> {
                    if (tokenStarted) {
                        tokens += current.toString()
                        current.clear()
                        tokenStarted = false
                    }
                }

                else -> {
                    current.append(char)
                    tokenStarted = true
                }
            }
        }

        if (escaping) {
            current.append('\\')
        }
        if (tokenStarted) {
            tokens += current.toString()
        }

        return tokens
    }
}
