package com.aterminal.app.metadata

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aterminal.app.agents.AgentType
import com.aterminal.app.data.AppDatabase
import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.data.WorkspaceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetadataTransferRepositoryTest {
    private lateinit var sourceDatabase: AppDatabase
    private lateinit var targetDatabase: AppDatabase

    @Before
    fun createDatabases() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sourceDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        targetDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabases() {
        sourceDatabase.close()
        targetDatabase.close()
    }

    @Test
    fun exportsHostAndWorkspaceMetadataWithoutSecrets() = runTest {
        val hostId = sourceDatabase.hostDao().insert(hostFixture())
        sourceDatabase.workspaceDao().insert(workspaceFixture(hostId = hostId))
        val repository = MetadataTransferRepository(
            hostDao = sourceDatabase.hostDao(),
            workspaceDao = sourceDatabase.workspaceDao(),
            clock = { 1_700_000_000_000L },
        )

        val payload = repository.exportMetadata()

        assertTrue(payload.contains("\"schemaVersion\":1"))
        assertTrue(payload.contains("\"displayName\":\"Dev Box\""))
        assertTrue(payload.contains("\"remoteCwd\":\"/srv/aterminal\""))
        assertTrue(payload.contains("\"agentFlags\":\"--model gpt-5.4\""))
        assertFalse(payload.contains("password", ignoreCase = true))
        assertFalse(payload.contains("privateKey", ignoreCase = true))
        assertFalse(payload.contains("secret", ignoreCase = true))
    }

    @Test
    fun importsMetadataAndRemapsWorkspaceHostIds() = runTest {
        val hostId = sourceDatabase.hostDao().insert(hostFixture())
        sourceDatabase.workspaceDao().insert(workspaceFixture(hostId = hostId))
        val payload = MetadataTransferRepository(
            hostDao = sourceDatabase.hostDao(),
            workspaceDao = sourceDatabase.workspaceDao(),
            clock = { 1_700_000_000_000L },
        ).exportMetadata()
        val targetRepository = MetadataTransferRepository(
            hostDao = targetDatabase.hostDao(),
            workspaceDao = targetDatabase.workspaceDao(),
            clock = { 1_700_000_100_000L },
        )

        val summary = targetRepository.importMetadata(payload)

        val importedHosts = targetDatabase.hostDao().observeAll().first()
        val importedWorkspaces = targetDatabase.workspaceDao()
            .observeForHost(importedHosts.single().id)
            .first()

        assertEquals(MetadataImportSummary(importedHosts = 1, importedWorkspaces = 1), summary)
        assertEquals("Dev Box", importedHosts.single().displayName)
        assertEquals(importedHosts.single().id, importedWorkspaces.single().hostId)
        assertEquals("/srv/aterminal", importedWorkspaces.single().remoteCwd)
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
        agentFlags = "--model gpt-5.4",
        createdAtEpochMillis = 200,
        updatedAtEpochMillis = 200,
    )
}
