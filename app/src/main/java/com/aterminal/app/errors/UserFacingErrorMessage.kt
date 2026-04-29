package com.aterminal.app.errors

import com.aterminal.app.agents.AgentType

object UserFacingErrorMessage {
    fun from(error: Throwable): String {
        return when (error) {
            is UserFacingException -> error.userMessage
            else -> error.message?.takeIf { it.isNotBlank() }
                ?: "Something went wrong. Retry the action or check the remote host."
        }
    }

    fun missingAgent(agentType: AgentType): String {
        return when (agentType) {
            AgentType.CODEX -> missingTool(RemoteTool.CODEX)
            AgentType.CLAUDE -> missingTool(RemoteTool.CLAUDE)
            AgentType.SHELL -> "Shell is provided by the remote login shell."
        }
    }

    fun missingTool(tool: RemoteTool): String {
        return when (tool) {
            RemoteTool.TMUX ->
                "tmux is not installed on the remote host. Install tmux before launching persistent agent sessions."

            RemoteTool.CODEX ->
                "Codex is not installed on the remote host. Install the codex CLI or choose Shell/Claude."

            RemoteTool.CLAUDE ->
                "Claude Code is not installed on the remote host. Install claude or choose Shell/Codex."
        }
    }
}

interface UserFacingException {
    val userMessage: String
}

class SshAuthenticationFailedException(
    cause: Throwable? = null,
) : RuntimeException("SSH authentication failed.", cause),
    UserFacingException {
    override val userMessage: String =
        "SSH authentication failed. Check the username, password, private key, or passphrase."
}

class HostKeyMismatchException(
    private val expectedFingerprint: String?,
    private val actualFingerprint: String?,
    cause: Throwable? = null,
) : RuntimeException("SSH host key mismatch.", cause),
    UserFacingException {
    override val userMessage: String = buildString {
        append("Host key changed for this host. Verify the fingerprint before connecting")
        expectedFingerprint?.let { append("; expected $it") }
        actualFingerprint?.let { append(", saw $it") }
        append(".")
    }
}

class MissingRemoteToolException(
    private val tool: RemoteTool,
) : RuntimeException("Missing remote tool: ${tool.commandName}"),
    UserFacingException {
    override val userMessage: String = UserFacingErrorMessage.missingTool(tool)
}

class TmuxCommandFailedException(
    val command: String,
    val exitStatus: Int,
    val stderr: String,
) : RuntimeException("tmux command failed with exit status $exitStatus: $stderr"),
    UserFacingException {
    override val userMessage: String =
        "tmux command failed: ${stderr.ifBlank { "exit status $exitStatus" }}"
}

enum class RemoteTool(
    val commandName: String,
) {
    TMUX("tmux"),
    CODEX("codex"),
    CLAUDE("claude"),
}
