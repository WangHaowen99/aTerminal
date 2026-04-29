package com.aterminal.app.agents

import com.aterminal.app.data.AgentSessionDao
import com.aterminal.app.data.AgentSessionEntity
import kotlinx.coroutines.flow.Flow

class AgentSessionRepository(
    private val agentSessionDao: AgentSessionDao,
) : AgentSessionStore {
    fun observeForWorkspace(workspaceId: Long): Flow<List<AgentSessionEntity>> {
        return agentSessionDao.observeForWorkspace(workspaceId)
    }

    suspend fun get(id: Long): AgentSessionEntity? {
        return agentSessionDao.getById(id)
    }

    suspend fun create(session: AgentSessionEntity): Long {
        return agentSessionDao.insert(session)
    }

    override suspend fun saveLaunchedSession(session: AgentSessionEntity): Long {
        val existing = agentSessionDao.getByWorkspaceAgentAndTmuxSession(
            workspaceId = session.workspaceId,
            agentType = session.agentType,
            tmuxSessionName = session.tmuxSessionName,
        )
        if (existing == null) {
            return agentSessionDao.insert(session)
        }

        agentSessionDao.update(
            existing.copy(
                agentSessionRef = session.agentSessionRef ?: existing.agentSessionRef,
                lastAttachedAtEpochMillis = session.lastAttachedAtEpochMillis,
            ),
        )
        return existing.id
    }

    suspend fun update(session: AgentSessionEntity) {
        agentSessionDao.update(session)
    }

    suspend fun delete(id: Long) {
        agentSessionDao.deleteById(id)
    }
}
