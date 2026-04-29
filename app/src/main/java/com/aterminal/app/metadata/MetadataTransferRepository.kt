package com.aterminal.app.metadata

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostDao
import com.aterminal.app.data.HostEntity
import com.aterminal.app.data.WorkspaceDao
import com.aterminal.app.data.WorkspaceEntity
import com.aterminal.app.agents.AgentType
import org.json.JSONArray
import org.json.JSONObject

class MetadataTransferRepository(
    private val hostDao: HostDao,
    private val workspaceDao: WorkspaceDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun exportMetadata(): String {
        val export = MetadataExport(
            exportedAtEpochMillis = clock(),
            hosts = hostDao.getAllSnapshot().map(MetadataHost::fromEntity),
            workspaces = workspaceDao.getAllSnapshot().map(MetadataWorkspace::fromEntity),
        )
        return MetadataExportCodec.encode(export)
    }

    suspend fun importMetadata(payload: String): MetadataImportSummary {
        val export = MetadataExportCodec.decode(payload)
        val hostIdMap = mutableMapOf<Long, Long>()
        var importedHosts = 0
        var importedWorkspaces = 0

        export.hosts.forEach { host ->
            val newId = hostDao.insert(host.toEntity(id = 0))
            hostIdMap[host.sourceId] = newId
            importedHosts += 1
        }

        export.workspaces.forEach { workspace ->
            val remappedHostId = hostIdMap[workspace.sourceHostId] ?: return@forEach
            workspaceDao.insert(workspace.toEntity(id = 0, hostId = remappedHostId))
            importedWorkspaces += 1
        }

        return MetadataImportSummary(
            importedHosts = importedHosts,
            importedWorkspaces = importedWorkspaces,
        )
    }
}

data class MetadataImportSummary(
    val importedHosts: Int,
    val importedWorkspaces: Int,
)

data class MetadataExport(
    val schemaVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val hosts: List<MetadataHost>,
    val workspaces: List<MetadataWorkspace>,
)

data class MetadataHost(
    val sourceId: Long,
    val displayName: String,
    val hostname: String,
    val port: Int,
    val username: String,
    val authType: AuthType,
    val pinnedFingerprint: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toEntity(id: Long): HostEntity {
        return HostEntity(
            id = id,
            displayName = displayName,
            hostname = hostname,
            port = port,
            username = username,
            authType = authType,
            pinnedFingerprint = pinnedFingerprint,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }

    companion object {
        fun fromEntity(host: HostEntity): MetadataHost {
            return MetadataHost(
                sourceId = host.id,
                displayName = host.displayName,
                hostname = host.hostname,
                port = host.port,
                username = host.username,
                authType = host.authType,
                pinnedFingerprint = host.pinnedFingerprint,
                createdAtEpochMillis = host.createdAtEpochMillis,
                updatedAtEpochMillis = host.updatedAtEpochMillis,
            )
        }
    }
}

data class MetadataWorkspace(
    val sourceId: Long,
    val sourceHostId: Long,
    val name: String,
    val remoteCwd: String,
    val preferredAgent: AgentType,
    val agentFlags: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toEntity(id: Long, hostId: Long): WorkspaceEntity {
        return WorkspaceEntity(
            id = id,
            hostId = hostId,
            name = name,
            remoteCwd = remoteCwd,
            preferredAgent = preferredAgent,
            agentFlags = agentFlags,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }

    companion object {
        fun fromEntity(workspace: WorkspaceEntity): MetadataWorkspace {
            return MetadataWorkspace(
                sourceId = workspace.id,
                sourceHostId = workspace.hostId,
                name = workspace.name,
                remoteCwd = workspace.remoteCwd,
                preferredAgent = workspace.preferredAgent,
                agentFlags = workspace.agentFlags,
                createdAtEpochMillis = workspace.createdAtEpochMillis,
                updatedAtEpochMillis = workspace.updatedAtEpochMillis,
            )
        }
    }
}

object MetadataExportCodec {
    fun encode(export: MetadataExport): String {
        return JSONObject()
            .put("schemaVersion", export.schemaVersion)
            .put("exportedAtEpochMillis", export.exportedAtEpochMillis)
            .put(
                "hosts",
                JSONArray().apply {
                    export.hosts.forEach { put(it.toJson()) }
                },
            )
            .put(
                "workspaces",
                JSONArray().apply {
                    export.workspaces.forEach { put(it.toJson()) }
                },
            )
            .toString()
            .replace("\\/", "/")
    }

    fun decode(payload: String): MetadataExport {
        val root = JSONObject(payload)
        val schemaVersion = root.getInt("schemaVersion")
        require(schemaVersion == 1) {
            "Unsupported metadata schema version: $schemaVersion"
        }
        return MetadataExport(
            schemaVersion = schemaVersion,
            exportedAtEpochMillis = root.getLong("exportedAtEpochMillis"),
            hosts = root.getJSONArray("hosts").mapObjects { it.toMetadataHost() },
            workspaces = root.getJSONArray("workspaces").mapObjects { it.toMetadataWorkspace() },
        )
    }

    private fun MetadataHost.toJson(): JSONObject {
        return JSONObject()
            .put("sourceId", sourceId)
            .put("displayName", displayName)
            .put("hostname", hostname)
            .put("port", port)
            .put("username", username)
            .put("authType", authType.name)
            .putNullable("pinnedFingerprint", pinnedFingerprint)
            .put("createdAtEpochMillis", createdAtEpochMillis)
            .put("updatedAtEpochMillis", updatedAtEpochMillis)
    }

    private fun MetadataWorkspace.toJson(): JSONObject {
        return JSONObject()
            .put("sourceId", sourceId)
            .put("sourceHostId", sourceHostId)
            .put("name", name)
            .put("remoteCwd", remoteCwd)
            .put("preferredAgent", preferredAgent.name)
            .putNullable("agentFlags", agentFlags)
            .put("createdAtEpochMillis", createdAtEpochMillis)
            .put("updatedAtEpochMillis", updatedAtEpochMillis)
    }

    private fun JSONObject.toMetadataHost(): MetadataHost {
        return MetadataHost(
            sourceId = getLong("sourceId"),
            displayName = getString("displayName"),
            hostname = getString("hostname"),
            port = getInt("port"),
            username = getString("username"),
            authType = AuthType.valueOf(getString("authType")),
            pinnedFingerprint = optionalString("pinnedFingerprint"),
            createdAtEpochMillis = getLong("createdAtEpochMillis"),
            updatedAtEpochMillis = getLong("updatedAtEpochMillis"),
        )
    }

    private fun JSONObject.toMetadataWorkspace(): MetadataWorkspace {
        return MetadataWorkspace(
            sourceId = getLong("sourceId"),
            sourceHostId = getLong("sourceHostId"),
            name = getString("name"),
            remoteCwd = getString("remoteCwd"),
            preferredAgent = AgentType.valueOf(getString("preferredAgent")),
            agentFlags = optionalString("agentFlags"),
            createdAtEpochMillis = getLong("createdAtEpochMillis"),
            updatedAtEpochMillis = getLong("updatedAtEpochMillis"),
        )
    }

    private fun JSONObject.putNullable(name: String, value: String?): JSONObject {
        return put(name, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optionalString(name: String): String? {
        return if (isNull(name)) {
            null
        } else {
            getString(name)
        }
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        return List(length()) { index -> transform(getJSONObject(index)) }
    }
}
