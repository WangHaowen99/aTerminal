package com.aterminal.app.ssh

interface LastAttachedTmuxSessionStore {
    suspend fun saveLastAttachedSession(hostId: Long, sessionName: String)

    suspend fun getLastAttachedSession(hostId: Long): String?
}

class InMemoryLastAttachedTmuxSessionStore : LastAttachedTmuxSessionStore {
    private val sessionsByHost = mutableMapOf<Long, String>()

    override suspend fun saveLastAttachedSession(hostId: Long, sessionName: String) {
        sessionsByHost[hostId] = sessionName
    }

    override suspend fun getLastAttachedSession(hostId: Long): String? {
        return sessionsByHost[hostId]
    }
}
