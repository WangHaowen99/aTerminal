package com.aterminal.app.agents

import com.aterminal.app.data.AgentSessionDao
import com.aterminal.app.data.AgentSessionEntity
import kotlinx.coroutines.flow.Flow

class AgentSessionRepository(
    private val agentSessionDao: AgentSessionDao,
) {
    fun observeForWorkspace(workspaceId: Long): Flow<List<AgentSessionEntity>> {
        return agentSessionDao.observeForWorkspace(workspaceId)
    }

    suspend fun get(id: Long): AgentSessionEntity? {
        return agentSessionDao.getById(id)
    }

    suspend fun create(session: AgentSessionEntity): Long {
        return agentSessionDao.insert(session)
    }

    suspend fun update(session: AgentSessionEntity) {
        agentSessionDao.update(session)
    }

    suspend fun delete(id: Long) {
        agentSessionDao.deleteById(id)
    }
}
