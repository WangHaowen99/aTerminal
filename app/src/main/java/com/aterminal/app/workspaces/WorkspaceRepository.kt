package com.aterminal.app.workspaces

import com.aterminal.app.data.WorkspaceDao
import com.aterminal.app.data.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

class WorkspaceRepository(
    private val workspaceDao: WorkspaceDao,
) : WorkspaceStore {
    override fun observeForHost(hostId: Long): Flow<List<WorkspaceEntity>> {
        return workspaceDao.observeForHost(hostId)
    }

    suspend fun get(id: Long): WorkspaceEntity? {
        return workspaceDao.getById(id)
    }

    suspend fun create(workspace: WorkspaceEntity): Long {
        return workspaceDao.insert(workspace)
    }

    suspend fun update(workspace: WorkspaceEntity) {
        workspaceDao.update(workspace)
    }

    suspend fun delete(id: Long) {
        workspaceDao.deleteById(id)
    }
}

interface WorkspaceStore {
    fun observeForHost(hostId: Long): Flow<List<WorkspaceEntity>>
}
