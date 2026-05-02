package com.aterminal.app.tmux

class TmuxCommandBuilder(
    private val quoter: (String) -> String = ShellQuoter::quote,
) {
    fun listSessions(): String {
        return "tmux list-sessions -F ${quoter(LIST_SESSIONS_FORMAT)}"
    }

    fun hasSession(sessionName: String): String {
        return "tmux has-session -t ${quoter(sessionName)}"
    }

    fun attachSession(sessionName: String): String {
        return "tmux attach-session -t ${quoter(sessionName)}"
    }

    fun newSession(
        sessionName: String,
        remoteCwd: String,
        command: String,
        detached: Boolean = false,
    ): String {
        val lifecycleFlag = if (detached) {
            "tmux new-session -Ads"
        } else {
            "tmux new-session -As"
        }
        return listOf(
            lifecycleFlag,
            quoter(sessionName),
            "-c",
            quoter(remoteCwd),
            quoter(command),
        ).joinToString(separator = " ")
    }

    fun killSession(sessionName: String): String {
        return "tmux kill-session -t ${quoter(sessionName)}"
    }

    fun detachClient(): String {
        return "tmux detach-client"
    }

    fun capturePane(
        sessionName: String,
        startLine: Int = DEFAULT_CAPTURE_START_LINE,
    ): String {
        return "tmux capture-pane -t ${quoter(sessionName)} -p -J -S $startLine"
    }

    private companion object {
        const val DEFAULT_CAPTURE_START_LINE = -3000
        const val LIST_SESSIONS_FORMAT =
            "#{session_name}\\t#{session_created}\\t#{session_attached}\\t#{session_windows}"
    }
}
