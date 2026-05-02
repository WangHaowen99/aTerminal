package com.aterminal.app.tmux

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aterminal.app.errors.UserFacingErrorMessage
import com.aterminal.app.terminal.TerminalSessionSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TmuxSessionListViewModel(
    private val manager: TmuxSessionManager,
    private val terminalAttacher: TmuxTerminalAttacher? = null,
    private val terminalSessionSink: TerminalSessionSink? = null,
    private val onAttachedToTerminal: () -> Unit = {},
    private val scope: CoroutineScope? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TmuxSessionListUiState())
    val state: StateFlow<TmuxSessionListUiState> = mutableState.asStateFlow()

    private val modelScope: CoroutineScope
        get() = scope ?: viewModelScope

    fun refresh() {
        modelScope.launch {
            mutableState.update {
                it.copy(loading = true, errorMessage = null)
            }
            runCatching { manager.listSessions() }
                .onSuccess { sessions ->
                    mutableState.update {
                        it.copy(loading = false, sessions = sessions)
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            loading = false,
                            errorMessage = UserFacingErrorMessage.from(error),
                        )
                    }
                }
        }
    }

    fun attachSession(sessionName: String) {
        modelScope.launch {
            runCatching {
                val attacher = terminalAttacher
                val terminalSink = terminalSessionSink
                if (attacher != null && terminalSink != null) {
                    terminalSink.attach(attacher.attach(sessionName))
                    onAttachedToTerminal()
                } else {
                    manager.attachSession(sessionName)
                }
            }.onFailure(::showError)
        }
    }

    fun requestKill(sessionName: String) {
        mutableState.update {
            it.copy(
                pendingKillSessionName = sessionName,
                killConfirmationRequired = true,
            )
        }
    }

    fun confirmKill() {
        val sessionName = mutableState.value.pendingKillSessionName ?: return
        mutableState.update {
            it.copy(
                pendingKillSessionName = null,
                killConfirmationRequired = false,
            )
        }
        modelScope.launch {
            runCatching { manager.killSession(sessionName) }
                .onSuccess { refresh() }
                .onFailure(::showError)
        }
    }

    fun cancelKill() {
        mutableState.update {
            it.copy(
                pendingKillSessionName = null,
                killConfirmationRequired = false,
            )
        }
    }

    fun detachClient() {
        modelScope.launch {
            runCatching { manager.detachClient() }
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        mutableState.update {
            it.copy(errorMessage = UserFacingErrorMessage.from(error))
        }
    }

    class Factory(
        private val manager: TmuxSessionManager,
        private val terminalAttacher: TmuxTerminalAttacher? = null,
        private val terminalSessionSink: TerminalSessionSink? = null,
        private val onAttachedToTerminal: () -> Unit = {},
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TmuxSessionListViewModel::class.java))
            return TmuxSessionListViewModel(
                manager = manager,
                terminalAttacher = terminalAttacher,
                terminalSessionSink = terminalSessionSink,
                onAttachedToTerminal = onAttachedToTerminal,
            ) as T
        }
    }
}

data class TmuxSessionListUiState(
    val sessions: List<TmuxSession> = emptyList(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val pendingKillSessionName: String? = null,
    val killConfirmationRequired: Boolean = false,
)
