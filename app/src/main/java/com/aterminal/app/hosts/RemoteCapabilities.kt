package com.aterminal.app.hosts

data class RemoteCapabilities(
    val tmux: RemoteToolCapability,
    val codex: RemoteToolCapability,
    val claude: RemoteToolCapability,
) {
    val tmuxInstallGuidance: String?
        get() = if (tmux.available) {
            null
        } else {
            "Install tmux on the remote host before launching persistent agent sessions."
        }
}

data class RemoteToolCapability(
    val available: Boolean,
    val path: String? = null,
    val version: String? = null,
)
