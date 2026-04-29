package com.aterminal.app.agents

enum class AgentType(
    val remoteName: String,
    val displayName: String,
) {
    CODEX("codex", "Codex"),
    CLAUDE("claude", "Claude"),
    SHELL("shell", "Shell"),
}
