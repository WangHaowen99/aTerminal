package com.aterminal.app.hosts

import com.aterminal.app.data.HostDao
import com.aterminal.app.data.HostEntity
import kotlinx.coroutines.flow.Flow

class HostRepository(
    private val hostDao: HostDao,
) {
    fun observeHosts(): Flow<List<HostEntity>> {
        return hostDao.observeAll()
    }

    suspend fun get(id: Long): HostEntity? {
        return hostDao.getById(id)
    }

    suspend fun create(host: HostEntity): Long {
        return hostDao.insert(host)
    }

    suspend fun update(host: HostEntity) {
        hostDao.update(host)
    }

    suspend fun delete(id: Long) {
        hostDao.deleteById(id)
    }
}
