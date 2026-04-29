package com.aterminal.app.agents

enum class AgentType(
    val remoteName: String,
) {
    CODEX("codex"),
    CLAUDE("claude"),
    SHELL("shell"),
}
