package com.aterminal.app.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.errors.UserFacingErrorMessage
import com.aterminal.app.security.HostCredentialSecret
import com.aterminal.app.security.HostCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HostListViewModel(
    private val hostStore: HostStore,
    private val credentialStore: HostCredentialStore,
    private val scope: CoroutineScope? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(HostListUiState())
    val state: StateFlow<HostListUiState> = mutableState.asStateFlow()

    private val modelScope: CoroutineScope
        get() = scope ?: viewModelScope

    init {
        modelScope.launch {
            hostStore.observeHosts().collect { hosts ->
                mutableState.update { it.copy(hosts = hosts) }
            }
        }
    }

    fun showAddHostDialog() {
        mutableState.update {
            it.copy(
                showAddHostDialog = true,
                errorMessage = null,
            )
        }
    }

    fun dismissAddHostDialog() {
        mutableState.update {
            it.copy(
                showAddHostDialog = false,
                form = HostFormState(),
                errorMessage = null,
            )
        }
    }

    fun updateForm(form: HostFormState) {
        mutableState.update {
            it.copy(
                form = form,
                errorMessage = null,
            )
        }
    }

    fun saveHost() {
        val form = mutableState.value.form
        val hostAndCredential = runCatching {
            HostAndCredential(
                host = form.toEntity(clock()),
                credential = form.toCredential(),
            )
        }
            .onFailure { error ->
                mutableState.update {
                    it.copy(errorMessage = UserFacingErrorMessage.from(error))
                }
            }
            .getOrNull()
            ?: return

        modelScope.launch {
            runCatching {
                val hostId = hostStore.create(hostAndCredential.host)
                runCatching {
                    credentialStore.saveCredential(
                        hostId = hostId,
                        credential = hostAndCredential.credential,
                    )
                }.onFailure {
                    hostStore.delete(hostId)
                }.getOrThrow()
            }
                .onSuccess { dismissAddHostDialog() }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(errorMessage = UserFacingErrorMessage.from(error))
                    }
                }
        }
    }

    fun deleteHost(id: Long) {
        modelScope.launch {
            runCatching {
                hostStore.delete(id)
                credentialStore.deleteCredential(id)
            }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(errorMessage = UserFacingErrorMessage.from(error))
                    }
                }
        }
    }

    class Factory(
        private val hostStore: HostStore,
        private val credentialStore: HostCredentialStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HostListViewModel::class.java))
            return HostListViewModel(
                hostStore = hostStore,
                credentialStore = credentialStore,
            ) as T
        }
    }
}

private data class HostAndCredential(
    val host: HostEntity,
    val credential: HostCredentialSecret,
)

data class HostListUiState(
    val hosts: List<HostEntity> = emptyList(),
    val form: HostFormState = HostFormState(),
    val showAddHostDialog: Boolean = false,
    val errorMessage: String? = null,
)

data class HostFormState(
    val displayName: String = "",
    val hostname: String = "",
    val portText: String = "22",
    val username: String = "",
    val authType: AuthType = AuthType.PRIVATE_KEY,
    val password: String = "",
    val privateKeyPem: String = "",
    val privateKeyPassphrase: String = "",
) {
    fun toEntity(nowEpochMillis: Long): HostEntity {
        val port = portText.toIntOrNull()
            ?: throw IllegalArgumentException("Port must be between 1 and 65535.")
        require(port in MIN_PORT..MAX_PORT) {
            "Port must be between 1 and 65535."
        }

        val trimmedDisplayName = displayName.trim()
        val trimmedHostname = hostname.trim()
        val trimmedUsername = username.trim()
        require(trimmedDisplayName.isNotEmpty()) {
            "Display name is required."
        }
        require(trimmedHostname.isNotEmpty()) {
            "Hostname is required."
        }
        require(trimmedUsername.isNotEmpty()) {
            "Username is required."
        }

        return HostEntity(
            displayName = trimmedDisplayName,
            hostname = trimmedHostname,
            port = port,
            username = trimmedUsername,
            authType = authType,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
        )
    }

    fun toCredential(): HostCredentialSecret {
        return when (authType) {
            AuthType.PASSWORD -> {
                require(password.isNotBlank()) {
                    "Password is required."
                }
                HostCredentialSecret.Password(password)
            }

            AuthType.PRIVATE_KEY -> {
                require(privateKeyPem.isNotBlank()) {
                    "Private key is required."
                }
                HostCredentialSecret.PrivateKey(
                    privateKeyPem = privateKeyPem,
                    passphrase = privateKeyPassphrase.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    private companion object {
        const val MIN_PORT = 1
        const val MAX_PORT = 65_535
    }
}
