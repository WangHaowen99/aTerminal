package com.aterminal.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aterminal.app.agents.AgentSessionRepository
import com.aterminal.app.agents.AgentType
import com.aterminal.app.hosts.HostRepository
import com.aterminal.app.workspaces.WorkspaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalDataModelTest {
    private lateinit var database: AppDatabase
    private lateinit var hostRepository: HostRepository
    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var agentSessionRepository: AgentSessionRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        hostRepository = HostRepository(database.hostDao())
        workspaceRepository = WorkspaceRepository(database.workspaceDao())
        agentSessionRepository = AgentSessionRepository(database.agentSessionDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun hostRepositoryCreatesUpdatesDeletesAndListsHosts() = runTest {
        val hostId = hostRepository.create(hostFixture())

        assertEquals(
            listOf("Dev Box"),
            hostRepository.observeHosts().first().map { it.displayName },
        )

        val savedHost = requireNotNull(hostRepository.get(hostId))
        hostRepository.update(
            savedHost.copy(
                displayName = "Production Box",
                pinnedFingerprint = "SHA256:def456",
                updatedAtEpochMillis = 300,
            ),
        )

        val updatedHost = requireNotNull(hostRepository.get(hostId))
        assertEquals("Production Box", updatedHost.displayName)
        assertEquals("SHA256:def456", updatedHost.pinnedFingerprint)

        hostRepository.delete(hostId)

        assertNull(hostRepository.get(hostId))
        assertTrue(hostRepository.observeHosts().first().isEmpty())
    }

    @Test
    fun workspaceRepositoryCreatesUpdatesDeletesAndListsWorkspaces() = runTest {
        val hostId = hostRepository.create(hostFixture())

        val workspaceId = workspaceRepository.create(
            workspaceFixture(hostId = hostId),
        )

        assertEquals(
            listOf("/srv/aterminal"),
            workspaceRepository.observeForHost(hostId).first().map { it.remoteCwd },
        )

        val savedWorkspace = requireNotNull(workspaceRepository.get(workspaceId))
        workspaceRepository.update(
            savedWorkspace.copy(
                remoteCwd = "/srv/aterminal/app",
                preferredAgent = AgentType.CLAUDE,
                agentFlags = "--dangerously-skip-permissions",
                updatedAtEpochMillis = 400,
            ),
        )

        val updatedWorkspace = requireNotNull(workspaceRepository.get(workspaceId))
        assertEquals(AgentType.CLAUDE, updatedWorkspace.preferredAgent)
        assertEquals("--dangerously-skip-permissions", updatedWorkspace.agentFlags)

        workspaceRepository.delete(workspaceId)

        assertNull(workspaceRepository.get(workspaceId))
        assertTrue(workspaceRepository.observeForHost(hostId).first().isEmpty())
    }

    @Test
    fun agentSessionRepositoryCreatesUpdatesDeletesAndListsSessions() = runTest {
        val hostId = hostRepository.create(hostFixture())
        val workspaceId = workspaceRepository.create(workspaceFixture(hostId = hostId))

        val sessionId = agentSessionRepository.create(
            AgentSessionEntity(
                hostId = hostId,
                workspaceId = workspaceId,
                agentType = AgentType.CODEX,
                tmuxSessionName = "aterminal-codex",
                agentSessionRef = "codex-session-1",
                lastAttachedAtEpochMillis = 500,
            ),
        )

        assertEquals(
            listOf("aterminal-codex"),
            agentSessionRepository.observeForWorkspace(workspaceId).first().map {
                it.tmuxSessionName
            },
        )

        val savedSession = requireNotNull(agentSessionRepository.get(sessionId))
        agentSessionRepository.update(
            savedSession.copy(
                agentSessionRef = "codex-session-2",
                lastAttachedAtEpochMillis = 600,
            ),
        )

        val updatedSession = requireNotNull(agentSessionRepository.get(sessionId))
        assertEquals("codex-session-2", updatedSession.agentSessionRef)
        assertEquals(600, updatedSession.lastAttachedAtEpochMillis)

        agentSessionRepository.delete(sessionId)

        assertNull(agentSessionRepository.get(sessionId))
        assertTrue(agentSessionRepository.observeForWorkspace(workspaceId).first().isEmpty())
    }

    private fun hostFixture() = HostEntity(
        displayName = "Dev Box",
        hostname = "dev.example.com",
        port = 22,
        username = "agent",
        authType = AuthType.PRIVATE_KEY,
        pinnedFingerprint = "SHA256:abc123",
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )

    private fun workspaceFixture(hostId: Long) = WorkspaceEntity(
        hostId = hostId,
        name = "aTerminal",
        remoteCwd = "/srv/aterminal",
        preferredAgent = AgentType.CODEX,
        agentFlags = "--no-alt-screen",
        createdAtEpochMillis = 200,
        updatedAtEpochMillis = 200,
    )
}
