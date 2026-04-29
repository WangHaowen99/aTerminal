package com.aterminal.app.settings

import com.aterminal.app.agents.WorkspaceAgentAction
import com.aterminal.app.data.WorkspaceEntity

class AgentLaunchFlagResolver(
    private val settings: AgentSettings,
) {
    fun flagsFor(
        workspace: WorkspaceEntity,
        action: WorkspaceAgentAction,
    ): List<String> {
        return settings.flagsFor(action.agentType) + AgentFlagParser.parse(workspace.agentFlags)
    }
}
