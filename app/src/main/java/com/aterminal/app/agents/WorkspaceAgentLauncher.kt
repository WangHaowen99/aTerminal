package com.aterminal.app.agents

import com.aterminal.app.data.AgentSessionEntity
import com.aterminal.app.data.WorkspaceEntity
import com.aterminal.app.errors.TmuxCommandFailedException
import com.aterminal.app.ssh.SshControlChannel
import com.aterminal.app.terminal.TerminalSessionSink
import com.aterminal.app.tmux.TmuxSessionManager
import com.aterminal.app.tmux.TmuxTerminalAttacher

class WorkspaceAgentLauncher(
    private val launchExecutor: AgentLaunchExecutor,
    private val sessionManager: TmuxSessionManager,
    private val sessionStore: AgentSessionStore,
    private val terminalAttacher: TmuxTerminalAttacher? = null,
    private val terminalSessionSink: TerminalSessionSink? = null,
    private val commandBuilder: AgentCommandBuilder = AgentCommandBuilder(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun launch(
        workspace: WorkspaceEntity,
        action: WorkspaceAgentAction,
        initialPrompt: String? = null,
        extraFlags: List<String> = emptyList(),
    ): WorkspaceAgentLaunchResult {
        val request = action.toLaunchRequest(
            workspace = workspace,
            initialPrompt = initialPrompt,
            extraFlags = extraFlags,
        )
        val launchCommand = commandBuilder.buildLaunchCommand(request)

        launchExecutor.executeLaunchCommand(launchCommand)
        val localSessionId = sessionStore.saveLaunchedSession(
            AgentSessionEntity(
                hostId = workspace.hostId,
                workspaceId = workspace.id,
                agentType = request.agentType,
                tmuxSessionName = request.tmuxSessionName,
                agentSessionRef = request.agentSessionRef,
                lastAttachedAtEpochMillis = clock(),
            ),
        )
        if (terminalAttacher != null && terminalSessionSink != null) {
            terminalSessionSink.attach(terminalAttacher.attach(request.tmuxSessionName))
        } else {
            sessionManager.attachSession(request.tmuxSessionName)
        }

        return WorkspaceAgentLaunchResult(
            localSessionId = localSessionId,
            tmuxSessionName = request.tmuxSessionName,
            launchCommand = launchCommand,
        )
    }
}

data class WorkspaceAgentLaunchResult(
    val localSessionId: Long,
    val tmuxSessionName: String,
    val launchCommand: String,
)

interface AgentLaunchExecutor {
    suspend fun executeLaunchCommand(command: String)
}

class SshAgentLaunchExecutor(
    private val controlChannel: SshControlChannel,
) : AgentLaunchExecutor {
    override suspend fun executeLaunchCommand(command: String) {
        val result = controlChannel.execute(command)
        if (result.exitStatus != 0) {
            throw TmuxCommandFailedException(
                command = command,
                exitStatus = result.exitStatus,
                stderr = result.stderr,
            )
        }
    }
}

interface AgentSessionStore {
    suspend fun saveLaunchedSession(session: AgentSessionEntity): Long
}
