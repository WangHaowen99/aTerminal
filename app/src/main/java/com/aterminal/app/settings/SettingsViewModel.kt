package com.aterminal.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aterminal.app.errors.UserFacingErrorMessage
import com.aterminal.app.metadata.MetadataTransferRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: AgentSettingsRepository,
    private val metadataTransferRepository: MetadataTransferRepository,
    private val scope: CoroutineScope? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    private val modelScope: CoroutineScope
        get() = scope ?: viewModelScope

    init {
        modelScope.launch {
            settingsRepository.settings.collect { settings ->
                mutableState.update {
                    it.copy(
                        codexDefaultFlags = settings.codexDefaultFlags,
                        claudeDefaultFlags = settings.claudeDefaultFlags,
                    )
                }
            }
        }
    }

    fun updateCodexDefaultFlags(flags: String) {
        mutableState.update { it.copy(codexDefaultFlags = flags) }
        modelScope.launch {
            settingsRepository.updateCodexDefaultFlags(flags)
        }
    }

    fun updateClaudeDefaultFlags(flags: String) {
        mutableState.update { it.copy(claudeDefaultFlags = flags) }
        modelScope.launch {
            settingsRepository.updateClaudeDefaultFlags(flags)
        }
    }

    fun exportMetadata() {
        modelScope.launch {
            runCatching { metadataTransferRepository.exportMetadata() }
                .onSuccess { payload ->
                    mutableState.update {
                        it.copy(
                            metadataExportPayload = payload,
                            statusMessage = "Metadata export is ready.",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(errorMessage = UserFacingErrorMessage.from(error))
                    }
                }
        }
    }

    fun updateImportPayload(payload: String) {
        mutableState.update { it.copy(importMetadataPayload = payload) }
    }

    fun importMetadata() {
        val payload = mutableState.value.importMetadataPayload
        modelScope.launch {
            runCatching { metadataTransferRepository.importMetadata(payload) }
                .onSuccess { summary ->
                    mutableState.update {
                        it.copy(
                            statusMessage = "Imported ${summary.importedHosts} hosts and ${summary.importedWorkspaces} workspaces.",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(errorMessage = UserFacingErrorMessage.from(error))
                    }
                }
        }
    }

    class Factory(
        private val settingsRepository: AgentSettingsRepository,
        private val metadataTransferRepository: MetadataTransferRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(
                settingsRepository = settingsRepository,
                metadataTransferRepository = metadataTransferRepository,
            ) as T
        }
    }
}

data class SettingsUiState(
    val codexDefaultFlags: String = "",
    val claudeDefaultFlags: String = "",
    val metadataExportPayload: String? = null,
    val importMetadataPayload: String = "",
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

const val PrivacyNotice: String =
    "Terminal content stays on this device and the remote host unless you explicitly export logs or metadata."
