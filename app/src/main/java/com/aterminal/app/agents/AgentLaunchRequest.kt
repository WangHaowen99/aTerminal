package com.aterminal.app.agents

data class AgentLaunchRequest(
    val agentType: AgentType,
    val mode: AgentLaunchMode = AgentLaunchMode.START,
    val workspaceCwd: String,
    val tmuxSessionName: String,
    val agentSessionRef: String? = null,
    val initialPrompt: String? = null,
    val extraFlags: List<String> = emptyList(),
)

enum class AgentLaunchMode {
    START,
    RESUME_LATEST,
    RESUME_BY_ID,
}
