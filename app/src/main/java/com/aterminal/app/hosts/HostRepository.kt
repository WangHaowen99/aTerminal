package com.aterminal.app.hosts

import com.aterminal.app.data.HostDao
import com.aterminal.app.data.HostEntity
import kotlinx.coroutines.flow.Flow

class HostRepository(
    private val hostDao: HostDao,
) : HostStore {
    override fun observeHosts(): Flow<List<HostEntity>> {
        return hostDao.observeAll()
    }

    suspend fun get(id: Long): HostEntity? {
        return hostDao.getById(id)
    }

    override suspend fun create(host: HostEntity): Long {
        return hostDao.insert(host)
    }

    suspend fun update(host: HostEntity) {
        hostDao.update(host)
    }

    override suspend fun delete(id: Long) {
        hostDao.deleteById(id)
    }
}

interface HostStore {
    fun observeHosts(): Flow<List<HostEntity>>

    suspend fun create(host: HostEntity): Long

    suspend fun delete(id: Long)
}
